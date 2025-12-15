package fr.kovelya.accounting.application.service.impl;

import fr.kovelya.accounting.application.service.AccountingService;
import fr.kovelya.accounting.application.service.InvoicePaymentService;
import fr.kovelya.accounting.domain.account.Account;
import fr.kovelya.accounting.domain.invoice.InvoiceStatus;
import fr.kovelya.accounting.domain.invoice.SalesInvoice;
import fr.kovelya.accounting.domain.invoice.SalesInvoiceId;
import fr.kovelya.accounting.domain.ledger.JournalType;
import fr.kovelya.accounting.domain.payment.CustomerPayment;
import fr.kovelya.accounting.domain.repository.AccountRepository;
import fr.kovelya.accounting.domain.repository.CustomerPaymentRepository;
import fr.kovelya.accounting.domain.repository.SalesInvoiceRepository;
import fr.kovelya.accounting.domain.shared.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class InvoicePaymentServiceImpl implements InvoicePaymentService {

    private final SalesInvoiceRepository salesInvoiceRepository;
    private final AccountRepository accountRepository;
    private final AccountingService accountingService;
    private final CustomerPaymentRepository customerPaymentRepository;
    private final String receivableAccountCode;
    private final IdempotencyExecutor idempotencyExecutor;

    public InvoicePaymentServiceImpl(SalesInvoiceRepository salesInvoiceRepository, AccountRepository accountRepository, AccountingService accountingService, CustomerPaymentRepository customerPaymentRepository, String receivableAccountCode, IdempotencyExecutor idempotencyExecutor) {
        this.salesInvoiceRepository = salesInvoiceRepository;
        this.accountRepository = accountRepository;
        this.accountingService = accountingService;
        this.customerPaymentRepository = customerPaymentRepository;
        this.receivableAccountCode = receivableAccountCode;
        this.idempotencyExecutor = idempotencyExecutor;
    }

    @Override
    public void recordPayment(UUID commandId, SalesInvoiceId invoiceId, String bankAccountCode, Money amount, LocalDate paymentDate) {
        idempotencyExecutor.runVoid(
                commandId,
                () -> doRecordPayment(invoiceId, bankAccountCode, amount, paymentDate),
                () -> {}
        );
    }

    private void doRecordPayment(SalesInvoiceId invoiceId, String bankAccountCode, Money amount, LocalDate paymentDate) {
        SalesInvoice invoice = salesInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        if (invoice.status() != InvoiceStatus.ISSUED && invoice.status() != InvoiceStatus.PARTIALLY_PAID) {
            throw new IllegalStateException("Only issued or partially paid invoices can be paid");
        }

        Money invoiceTotal = invoice.total();
        Money alreadyPaid = Money.zero(invoiceTotal.currency());

        List<CustomerPayment> existingPayments = customerPaymentRepository.findByInvoice(invoiceId);
        for (CustomerPayment payment : existingPayments) {
            alreadyPaid = alreadyPaid.add(payment.amount());
        }

        Money remaining = invoiceTotal.subtract(alreadyPaid);
        if (remaining.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Invoice is already fully paid");
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

        Account receivable = accountRepository.findByCode(receivableAccountCode)
                .orElseThrow(() -> new IllegalStateException("Receivable account not found: " + receivableAccountCode));

        Account bank = accountRepository.findByCode(bankAccountCode)
                .orElseThrow(() -> new IllegalStateException("Bank account not found: " + bankAccountCode));

        accountingService.postTransfer(
                bank.id(),
                receivable.id(),
                toPay,
                JournalType.BANK,
                "Payment of invoice " + invoice.number(),
                paymentDate
        );

        CustomerPayment payment = CustomerPayment.create(invoice.id(), toPay, paymentDate, bankAccountCode);
        customerPaymentRepository.save(payment);

        SalesInvoice updated;
        if (toPay.amount().compareTo(remaining.amount()) == 0) {
            updated = invoice.markPaid();
        } else {
            updated = invoice.markPartiallyPaid();
        }
        salesInvoiceRepository.save(updated);
    }
}
