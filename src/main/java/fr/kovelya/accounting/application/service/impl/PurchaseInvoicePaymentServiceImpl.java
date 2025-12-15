package fr.kovelya.accounting.application.service.impl;

import fr.kovelya.accounting.application.service.AccountingService;
import fr.kovelya.accounting.application.service.PurchaseInvoicePaymentService;
import fr.kovelya.accounting.domain.account.Account;
import fr.kovelya.accounting.domain.ledger.JournalType;
import fr.kovelya.accounting.domain.payment.SupplierPayment;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoice;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceId;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceStatus;
import fr.kovelya.accounting.domain.repository.AccountRepository;
import fr.kovelya.accounting.domain.repository.PurchaseInvoiceRepository;
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
    private final String payableAccountCode;
    private final IdempotencyExecutor idempotencyExecutor;

    public PurchaseInvoicePaymentServiceImpl(PurchaseInvoiceRepository purchaseInvoiceRepository, AccountRepository accountRepository, AccountingService accountingService, SupplierPaymentRepository supplierPaymentRepository, String payableAccountCode, IdempotencyExecutor idempotencyExecutor) {
        this.purchaseInvoiceRepository = purchaseInvoiceRepository;
        this.accountRepository = accountRepository;
        this.accountingService = accountingService;
        this.supplierPaymentRepository = supplierPaymentRepository;
        this.payableAccountCode = payableAccountCode;
        this.idempotencyExecutor = idempotencyExecutor;
    }

    @Override
    public void recordPayment(UUID commandId, PurchaseInvoiceId invoiceId, String bankAccountCode, Money amount, LocalDate paymentDate) {
        idempotencyExecutor.runVoid(
                commandId,
                () -> doRecordPayment(invoiceId, bankAccountCode, amount, paymentDate),
                () -> {}
        );
    }

    private void doRecordPayment(PurchaseInvoiceId invoiceId, String bankAccountCode, Money amount, LocalDate paymentDate) {
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

        Money toPay;
        if (amount == null) {
            toPay = remaining;
        } else {
            if (!amount.currency().equals(invoiceTotal.currency())) {
                throw new IllegalArgumentException("Payment currency must match invoice currency");
            }
            if (amount.amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Payment amount must be positive");
            }
            if (amount.amount().compareTo(remaining.amount()) > 0) {
                throw new IllegalArgumentException("Payment amount exceeds remaining balance");
            }
            toPay = amount;
        }

        Account payable = accountRepository.findByCode(payableAccountCode)
                .orElseThrow(() -> new IllegalStateException("Payable account not found: " + payableAccountCode));

        Account bank = accountRepository.findByCode(bankAccountCode)
                .orElseThrow(() -> new IllegalStateException("Bank account not found: " + bankAccountCode));

        accountingService.postTransfer(
                payable.id(),
                bank.id(),
                toPay,
                JournalType.BANK,
                "Payment of purchase invoice " + invoice.number(),
                paymentDate
        );

        SupplierPayment payment = SupplierPayment.create(invoice.id(), toPay, paymentDate, bankAccountCode);
        supplierPaymentRepository.save(payment);

        PurchaseInvoice updated;
        if (toPay.amount().compareTo(remaining.amount()) == 0) {
            updated = invoice.markPaid();
        } else {
            updated = invoice.markPartiallyPaid();
        }
        purchaseInvoiceRepository.save(updated);
    }
}
