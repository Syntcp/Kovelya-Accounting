package fr.kovelya.accounting.application.service.impl;

import fr.kovelya.accounting.application.dto.AccountPosting;
import fr.kovelya.accounting.application.dto.PurchaseInvoicePaymentAllocation;
import fr.kovelya.accounting.application.service.AccountingService;
import fr.kovelya.accounting.application.service.SupplierBatchPaymentService;
import fr.kovelya.accounting.domain.account.Account;
import fr.kovelya.accounting.domain.ledger.JournalType;
import fr.kovelya.accounting.domain.ledger.LedgerEntry;
import fr.kovelya.accounting.domain.payment.SupplierPayment;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoice;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceStatus;
import fr.kovelya.accounting.domain.repository.AccountRepository;
import fr.kovelya.accounting.domain.repository.PurchaseInvoiceRepository;
import fr.kovelya.accounting.domain.repository.SupplierPaymentRepository;
import fr.kovelya.accounting.domain.shared.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

public final class SupplierBatchPaymentServiceImpl implements SupplierBatchPaymentService {

    private final PurchaseInvoiceRepository purchaseInvoiceRepository;
    private final AccountRepository accountRepository;
    private final AccountingService accountingService;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final String payableAccountCode;
    private final IdempotencyExecutor idempotencyExecutor;

    public SupplierBatchPaymentServiceImpl(
            PurchaseInvoiceRepository purchaseInvoiceRepository,
            AccountRepository accountRepository,
            AccountingService accountingService,
            SupplierPaymentRepository supplierPaymentRepository,
            String payableAccountCode,
            IdempotencyExecutor idempotencyExecutor
    ) {
        this.purchaseInvoiceRepository = purchaseInvoiceRepository;
        this.accountRepository = accountRepository;
        this.accountingService = accountingService;
        this.supplierPaymentRepository = supplierPaymentRepository;
        this.payableAccountCode = payableAccountCode;
        this.idempotencyExecutor = idempotencyExecutor;
    }

    @Override
    public void recordBatchPayment(UUID commandId, String bankAccountCode, LocalDate paymentDate, PurchaseInvoicePaymentAllocation... allocations) {
        idempotencyExecutor.runVoid(
                commandId,
                () -> doRecord(commandId, bankAccountCode, paymentDate, allocations),
                () -> {}
        );
    }

    private void doRecord(UUID commandId, String bankAccountCode, LocalDate paymentDate, PurchaseInvoicePaymentAllocation... allocations) {
        if (allocations == null || allocations.length == 0) {
            throw new IllegalArgumentException("At least one allocation is required");
        }
        if (paymentDate == null) {
            throw new IllegalArgumentException("Payment date is required");
        }

        Account bank = accountRepository.findByCode(bankAccountCode)
                .orElseThrow(() -> new IllegalStateException("Bank account not found: " + bankAccountCode));

        Account payable = accountRepository.findByCode(payableAccountCode)
                .orElseThrow(() -> new IllegalStateException("Payable account not found: " + payableAccountCode));

        Currency currency = null;
        Money total = null;

        PurchaseInvoice firstInvoice = null;

        List<SupplierPayment> paymentsToSave = new ArrayList<>();
        List<PurchaseInvoice> invoicesToSave = new ArrayList<>();

        for (PurchaseInvoicePaymentAllocation a : allocations) {
            if (a == null || a.invoiceId() == null || a.amount() == null) {
                throw new IllegalArgumentException("Invalid allocation");
            }
            if (a.amount().amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Allocation amount must be positive");
            }

            PurchaseInvoice invoice = purchaseInvoiceRepository.findById(a.invoiceId())
                    .orElseThrow(() -> new IllegalArgumentException("Purchase invoice not found"));

            if (invoice.status() != PurchaseInvoiceStatus.ISSUED && invoice.status() != PurchaseInvoiceStatus.PARTIALLY_PAID) {
                throw new IllegalStateException("Only issued or partially paid purchase invoices can be paid");
            }

            if (firstInvoice == null) {
                firstInvoice = invoice;
            } else if (!invoice.supplierId().equals(firstInvoice.supplierId())) {
                throw new IllegalStateException("All purchase invoices must belong to the same supplier");
            }

            if (currency == null) {
                currency = a.amount().currency();
                total = Money.zero(currency);
            } else if (!currency.equals(a.amount().currency())) {
                throw new IllegalStateException("Multiple currencies not supported in batch payment");
            }

            Money alreadyPaid = Money.zero(invoice.total().currency());
            for (SupplierPayment p : supplierPaymentRepository.findByInvoice(invoice.id())) {
                alreadyPaid = alreadyPaid.add(p.amount());
            }

            Money remaining = invoice.total().subtract(alreadyPaid);
            if (remaining.amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("Purchase invoice already fully paid: " + invoice.number());
            }
            if (a.amount().amount().compareTo(remaining.amount()) > 0) {
                throw new IllegalArgumentException("Allocation exceeds remaining for purchase invoice: " + invoice.number());
            }

            total = total.add(a.amount());

            paymentsToSave.add(SupplierPayment.create(invoice.id(), a.amount(), paymentDate, bankAccountCode));

            PurchaseInvoice updated = a.amount().amount().compareTo(remaining.amount()) == 0
                    ? invoice.markPaid()
                    : invoice.markPartiallyPaid();

            invoicesToSave.add(updated);
        }

        AccountPosting debitPayable = new AccountPosting(payable.id(), total, LedgerEntry.Direction.DEBIT);
        AccountPosting creditBank = new AccountPosting(bank.id(), total, LedgerEntry.Direction.CREDIT);

        accountingService.postJournalTransaction(
                JournalType.BANK,
                "BANK-BATCH-SUP-" + commandId,
                "Supplier batch payment",
                paymentDate,
                debitPayable,
                creditBank
        );

        for (SupplierPayment p : paymentsToSave) {
            supplierPaymentRepository.save(p);
        }
        for (PurchaseInvoice inv : invoicesToSave) {
            purchaseInvoiceRepository.save(inv);
        }
    }
}
