package fr.kovelya.accounting.application.service.impl;

import fr.kovelya.accounting.application.dto.AccountPosting;
import fr.kovelya.accounting.application.service.AccountingService;
import fr.kovelya.accounting.application.service.SupplierAdvanceApplicationService;
import fr.kovelya.accounting.domain.account.Account;
import fr.kovelya.accounting.domain.advance.SupplierAdvance;
import fr.kovelya.accounting.domain.ledger.JournalType;
import fr.kovelya.accounting.domain.ledger.LedgerEntry;
import fr.kovelya.accounting.domain.payment.SupplierPayment;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoice;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceId;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceStatus;
import fr.kovelya.accounting.domain.repository.*;
import fr.kovelya.accounting.domain.shared.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class SupplierAdvanceApplicationServiceImpl implements SupplierAdvanceApplicationService {

    private final PurchaseInvoiceRepository purchaseInvoiceRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final SupplierAdvanceRepository supplierAdvanceRepository;
    private final AccountRepository accountRepository;
    private final AccountingService accountingService;
    private final String payableAccountCode;
    private final String supplierAdvanceAccountCode;
    private final IdempotencyExecutor idempotencyExecutor;

    public SupplierAdvanceApplicationServiceImpl(
            PurchaseInvoiceRepository purchaseInvoiceRepository,
            SupplierPaymentRepository supplierPaymentRepository,
            SupplierAdvanceRepository supplierAdvanceRepository,
            AccountRepository accountRepository,
            AccountingService accountingService,
            String payableAccountCode,
            String supplierAdvanceAccountCode,
            IdempotencyExecutor idempotencyExecutor
    ) {
        this.purchaseInvoiceRepository = purchaseInvoiceRepository;
        this.supplierPaymentRepository = supplierPaymentRepository;
        this.supplierAdvanceRepository = supplierAdvanceRepository;
        this.accountRepository = accountRepository;
        this.accountingService = accountingService;
        this.payableAccountCode = payableAccountCode;
        this.supplierAdvanceAccountCode = supplierAdvanceAccountCode;
        this.idempotencyExecutor = idempotencyExecutor;
    }

    @Override
    public void applyAdvance(UUID commandId, PurchaseInvoiceId invoiceId, Money amount, LocalDate date) {
        idempotencyExecutor.runVoid(
                commandId,
                () -> doApply(invoiceId, amount, date, commandId),
                () -> {}
        );
    }

    private void doApply(PurchaseInvoiceId invoiceId, Money amount, LocalDate date, UUID commandId) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        if (date == null) {
            throw new IllegalArgumentException("Date is required");
        }
        if (amount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        PurchaseInvoice invoice = purchaseInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase invoice not found"));

        if (invoice.status() != PurchaseInvoiceStatus.ISSUED && invoice.status() != PurchaseInvoiceStatus.PARTIALLY_PAID) {
            throw new IllegalStateException("Only issued or partially paid purchase invoices can be settled with advance");
        }

        if (!amount.currency().equals(invoice.total().currency())) {
            throw new IllegalArgumentException("Currency mismatch");
        }

        Money alreadyPaid = Money.zero(invoice.total().currency());
        for (SupplierPayment p : supplierPaymentRepository.findByInvoice(invoiceId)) {
            alreadyPaid = alreadyPaid.add(p.amount());
        }

        Money remaining = invoice.total().subtract(alreadyPaid);
        if (remaining.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Purchase invoice is already fully paid");
        }

        Money toApply = amount.amount().compareTo(remaining.amount()) > 0 ? remaining : amount;

        List<SupplierAdvance> advances = supplierAdvanceRepository.findOpenBySupplier(invoice.supplierId());
        Money available = Money.zero(toApply.currency());
        for (SupplierAdvance a : advances) {
            available = available.add(a.remaining());
        }
        if (available.amount().compareTo(toApply.amount()) < 0) {
            throw new IllegalStateException("Not enough supplier advance available");
        }

        Money left = toApply;
        for (SupplierAdvance a : advances) {
            if (left.amount().compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            Money canUse = a.remaining().amount().compareTo(left.amount()) > 0 ? left : a.remaining();
            supplierAdvanceRepository.save(a.consume(canUse));
            left = left.subtract(canUse);
        }

        Account payable = accountRepository.findByCode(payableAccountCode)
                .orElseThrow(() -> new IllegalStateException("Payable account not found: " + payableAccountCode));

        Account adv = accountRepository.findByCode(supplierAdvanceAccountCode)
                .orElseThrow(() -> new IllegalStateException("Supplier advances account not found: " + supplierAdvanceAccountCode));

        String reference = "ADV-APPLY-" + invoice.number() + "-" + commandId;

        AccountPosting debitPayable = new AccountPosting(payable.id(), toApply, LedgerEntry.Direction.DEBIT);
        AccountPosting creditAdv = new AccountPosting(adv.id(), toApply, LedgerEntry.Direction.CREDIT);

        accountingService.postJournalTransaction(
                JournalType.ADJUSTMENT,
                reference,
                "Apply supplier advance to purchase invoice " + invoice.number(),
                date,
                debitPayable,
                creditAdv
        );

        SupplierPayment synthetic = SupplierPayment.create(invoice.id(), toApply, date, "ADV-4091");
        supplierPaymentRepository.save(synthetic);

        PurchaseInvoice updated;
        if (toApply.amount().compareTo(remaining.amount()) == 0) {
            updated = invoice.markPaid();
        } else {
            updated = invoice.markPartiallyPaid();
        }
        purchaseInvoiceRepository.save(updated);
    }
}
