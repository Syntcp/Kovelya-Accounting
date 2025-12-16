package fr.kovelya.accounting.application.service.impl;

import fr.kovelya.accounting.application.dto.AccountPosting;
import fr.kovelya.accounting.application.dto.SalesInvoicePaymentAllocation;
import fr.kovelya.accounting.application.service.AccountingService;
import fr.kovelya.accounting.application.service.CustomerBatchPaymentService;
import fr.kovelya.accounting.domain.account.Account;
import fr.kovelya.accounting.domain.invoice.InvoiceStatus;
import fr.kovelya.accounting.domain.invoice.SalesInvoice;
import fr.kovelya.accounting.domain.ledger.JournalType;
import fr.kovelya.accounting.domain.ledger.LedgerEntry;
import fr.kovelya.accounting.domain.payment.CustomerPayment;
import fr.kovelya.accounting.domain.repository.AccountRepository;
import fr.kovelya.accounting.domain.repository.CustomerPaymentRepository;
import fr.kovelya.accounting.domain.repository.SalesInvoiceRepository;
import fr.kovelya.accounting.domain.shared.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

public final class CustomerBatchPaymentServiceImpl implements CustomerBatchPaymentService {

    private final SalesInvoiceRepository salesInvoiceRepository;
    private final AccountRepository accountRepository;
    private final AccountingService accountingService;
    private final CustomerPaymentRepository customerPaymentRepository;
    private final String receivableAccountCode;
    private final IdempotencyExecutor idempotencyExecutor;

    public CustomerBatchPaymentServiceImpl(
            SalesInvoiceRepository salesInvoiceRepository,
            AccountRepository accountRepository,
            AccountingService accountingService,
            CustomerPaymentRepository customerPaymentRepository,
            String receivableAccountCode,
            IdempotencyExecutor idempotencyExecutor
    ) {
        this.salesInvoiceRepository = salesInvoiceRepository;
        this.accountRepository = accountRepository;
        this.accountingService = accountingService;
        this.customerPaymentRepository = customerPaymentRepository;
        this.receivableAccountCode = receivableAccountCode;
        this.idempotencyExecutor = idempotencyExecutor;
    }

    @Override
    public void recordBatchPayment(UUID commandId, String bankAccountCode, LocalDate paymentDate, SalesInvoicePaymentAllocation... allocations) {
        idempotencyExecutor.runVoid(
                commandId,
                () -> doRecord(commandId, bankAccountCode, paymentDate, allocations),
                () -> {}
        );
    }

    private void doRecord(UUID commandId, String bankAccountCode, LocalDate paymentDate, SalesInvoicePaymentAllocation... allocations) {
        if (allocations == null || allocations.length == 0) {
            throw new IllegalArgumentException("At least one allocation is required");
        }
        if (paymentDate == null) {
            throw new IllegalArgumentException("Payment date is required");
        }

        Account bank = accountRepository.findByCode(bankAccountCode)
                .orElseThrow(() -> new IllegalStateException("Bank account not found: " + bankAccountCode));

        Account receivable = accountRepository.findByCode(receivableAccountCode)
                .orElseThrow(() -> new IllegalStateException("Receivable account not found: " + receivableAccountCode));

        Currency currency = null;
        Money total = null;

        SalesInvoice firstInvoice = null;

        List<CustomerPayment> paymentsToSave = new ArrayList<>();
        List<SalesInvoice> invoicesToSave = new ArrayList<>();

        for (SalesInvoicePaymentAllocation a : allocations) {
            if (a == null || a.invoiceId() == null || a.amount() == null) {
                throw new IllegalArgumentException("Invalid allocation");
            }
            if (a.amount().amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Allocation amount must be positive");
            }

            SalesInvoice invoice = salesInvoiceRepository.findById(a.invoiceId())
                    .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

            if (invoice.status() != InvoiceStatus.ISSUED && invoice.status() != InvoiceStatus.PARTIALLY_PAID) {
                throw new IllegalStateException("Only issued or partially paid invoices can be paid");
            }

            if (firstInvoice == null) {
                firstInvoice = invoice;
            } else if (!invoice.customerId().equals(firstInvoice.customerId())) {
                throw new IllegalStateException("All invoices must belong to the same customer");
            }

            if (currency == null) {
                currency = a.amount().currency();
                total = Money.zero(currency);
            } else if (!currency.equals(a.amount().currency())) {
                throw new IllegalStateException("Multiple currencies not supported in batch payment");
            }

            Money alreadyPaid = Money.zero(invoice.total().currency());
            for (CustomerPayment p : customerPaymentRepository.findByInvoice(invoice.id())) {
                alreadyPaid = alreadyPaid.add(p.amount());
            }

            Money remaining = invoice.total().subtract(alreadyPaid);
            if (remaining.amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("Invoice already fully paid: " + invoice.number());
            }
            if (a.amount().amount().compareTo(remaining.amount()) > 0) {
                throw new IllegalArgumentException("Allocation exceeds remaining for invoice: " + invoice.number());
            }

            total = total.add(a.amount());

            paymentsToSave.add(CustomerPayment.create(invoice.id(), a.amount(), paymentDate, bankAccountCode));

            SalesInvoice updated = a.amount().amount().compareTo(remaining.amount()) == 0
                    ? invoice.markPaid()
                    : invoice.markPartiallyPaid();

            invoicesToSave.add(updated);
        }

        AccountPosting debitBank = new AccountPosting(bank.id(), total, LedgerEntry.Direction.DEBIT);
        AccountPosting creditAr = new AccountPosting(receivable.id(), total, LedgerEntry.Direction.CREDIT);

        accountingService.postJournalTransaction(
                JournalType.BANK,
                "BANK-BATCH-CUST-" + commandId,
                "Customer batch payment",
                paymentDate,
                debitBank,
                creditAr
        );

        for (CustomerPayment p : paymentsToSave) {
            customerPaymentRepository.save(p);
        }
        for (SalesInvoice inv : invoicesToSave) {
            salesInvoiceRepository.save(inv);
        }
    }
}
