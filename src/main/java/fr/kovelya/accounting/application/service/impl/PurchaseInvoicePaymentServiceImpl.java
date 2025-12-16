package fr.kovelya.accounting.application.service.impl;

import fr.kovelya.accounting.application.dto.AccountPosting;
import fr.kovelya.accounting.application.service.AccountingService;
import fr.kovelya.accounting.application.service.PurchaseInvoicePaymentService;
import fr.kovelya.accounting.domain.account.Account;
import fr.kovelya.accounting.domain.advance.SupplierAdvance;
import fr.kovelya.accounting.domain.ledger.JournalType;
import fr.kovelya.accounting.domain.ledger.LedgerEntry;
import fr.kovelya.accounting.domain.payment.SupplierPayment;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoice;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceId;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceStatus;
import fr.kovelya.accounting.domain.repository.AccountRepository;
import fr.kovelya.accounting.domain.repository.PurchaseInvoiceRepository;
import fr.kovelya.accounting.domain.repository.SupplierAdvanceRepository;
import fr.kovelya.accounting.domain.repository.SupplierPaymentRepository;
import fr.kovelya.accounting.domain.shared.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class PurchaseInvoicePaymentServiceImpl implements PurchaseInvoicePaymentService {

    private final PurchaseInvoiceRepository purchaseInvoiceRepository;
    private final AccountRepository accountRepository;
    private final AccountingService accountingService;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final SupplierAdvanceRepository supplierAdvanceRepository;
    private final String payableAccountCode;
    private final String supplierAdvanceAccountCode;
    private final IdempotencyExecutor idempotencyExecutor;

    public PurchaseInvoicePaymentServiceImpl(
            PurchaseInvoiceRepository purchaseInvoiceRepository,
            AccountRepository accountRepository,
            AccountingService accountingService,
            SupplierPaymentRepository supplierPaymentRepository,
            SupplierAdvanceRepository supplierAdvanceRepository,
            String payableAccountCode,
            String supplierAdvanceAccountCode,
            IdempotencyExecutor idempotencyExecutor
    ) {
        this.purchaseInvoiceRepository = purchaseInvoiceRepository;
        this.accountRepository = accountRepository;
        this.accountingService = accountingService;
        this.supplierPaymentRepository = supplierPaymentRepository;
        this.supplierAdvanceRepository = supplierAdvanceRepository;
        this.payableAccountCode = payableAccountCode;
        this.supplierAdvanceAccountCode = supplierAdvanceAccountCode;
        this.idempotencyExecutor = idempotencyExecutor;
    }

    @Override
    public void recordPayment(UUID commandId, PurchaseInvoiceId invoiceId, String bankAccountCode, Money amount, LocalDate paymentDate) {
        idempotencyExecutor.runVoid(
                commandId,
                () -> doRecordPayment(commandId, invoiceId, bankAccountCode, amount, paymentDate),
                () -> {}
        );
    }

    private void doRecordPayment(UUID commandId, PurchaseInvoiceId invoiceId, String bankAccountCode, Money amount, LocalDate paymentDate) {
        PurchaseInvoice invoice = purchaseInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase invoice not found"));

        if (invoice.status() != PurchaseInvoiceStatus.ISSUED && invoice.status() != PurchaseInvoiceStatus.PARTIALLY_PAID) {
            throw new IllegalStateException("Only issued or partially paid purchase invoices can be paid");
        }

        Money invoiceTotal = invoice.total();
        Money alreadyPaid = Money.zero(invoiceTotal.currency());

        List<SupplierPayment> existingPayments = supplierPaymentRepository.findByInvoice(invoiceId);
        for (SupplierPayment payment : existingPayments) {
            alreadyPaid = alreadyPaid.add(payment.amount());
        }

        Money remaining = invoiceTotal.subtract(alreadyPaid);
        if (remaining.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Purchase invoice is already fully paid");
        }

        Money totalPaid;
        if (amount == null) {
            totalPaid = remaining;
        } else {
            if (!amount.currency().equals(invoiceTotal.currency())) {
                throw new IllegalArgumentException("Payment currency must match invoice currency");
            }
            if (amount.amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Payment amount must be positive");
            }
            totalPaid = amount;
        }

        Money applied = totalPaid.amount().compareTo(remaining.amount()) > 0 ? remaining : totalPaid;
        Money excess = totalPaid.subtract(applied);

        Account payable = accountRepository.findByCode(payableAccountCode)
                .orElseThrow(() -> new IllegalStateException("Payable account not found: " + payableAccountCode));

        Account bank = accountRepository.findByCode(bankAccountCode)
                .orElseThrow(() -> new IllegalStateException("Bank account not found: " + bankAccountCode));

        String reference = "BANK-PAY-" + invoice.number() + "-" + commandId;
        String description = "Payment of purchase invoice " + invoice.number();

        AccountPosting debitPayable = new AccountPosting(payable.id(), applied, LedgerEntry.Direction.DEBIT);
        AccountPosting creditBank = new AccountPosting(bank.id(), totalPaid, LedgerEntry.Direction.CREDIT);

        if (excess.amount().compareTo(BigDecimal.ZERO) > 0) {
            Account advances = accountRepository.findByCode(supplierAdvanceAccountCode)
                    .orElseThrow(() -> new IllegalStateException("Supplier advances account not found: " + supplierAdvanceAccountCode));

            AccountPosting debitAdvances = new AccountPosting(advances.id(), excess, LedgerEntry.Direction.DEBIT);

            accountingService.postJournalTransaction(
                    JournalType.BANK,
                    reference,
                    description,
                    paymentDate,
                    debitPayable,
                    debitAdvances,
                    creditBank
            );

            if (supplierAdvanceRepository.findBySourceCommandId(commandId).isEmpty()) {
                supplierAdvanceRepository.save(SupplierAdvance.create(invoice.supplierId(), excess, commandId));
            }
        } else {
            accountingService.postJournalTransaction(
                    JournalType.BANK,
                    reference,
                    description,
                    paymentDate,
                    debitPayable,
                    creditBank
            );
        }

        SupplierPayment payment = SupplierPayment.create(invoice.id(), applied, paymentDate, bankAccountCode);
        supplierPaymentRepository.save(payment);

        PurchaseInvoice updated;
        if (applied.amount().compareTo(remaining.amount()) == 0) {
            updated = invoice.markPaid();
        } else {
            updated = invoice.markPartiallyPaid();
        }
        purchaseInvoiceRepository.save(updated);
    }
}
