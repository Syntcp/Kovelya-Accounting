package fr.kovelya.accounting.application.service.impl;

import fr.kovelya.accounting.application.dto.AccountPosting;
import fr.kovelya.accounting.application.service.AccountingService;
import fr.kovelya.accounting.application.service.CustomerCreditApplicationService;
import fr.kovelya.accounting.domain.account.Account;
import fr.kovelya.accounting.domain.credit.CustomerCredit;
import fr.kovelya.accounting.domain.invoice.InvoiceStatus;
import fr.kovelya.accounting.domain.invoice.SalesInvoice;
import fr.kovelya.accounting.domain.invoice.SalesInvoiceId;
import fr.kovelya.accounting.domain.ledger.JournalType;
import fr.kovelya.accounting.domain.ledger.LedgerEntry;
import fr.kovelya.accounting.domain.payment.CustomerPayment;
import fr.kovelya.accounting.domain.repository.AccountRepository;
import fr.kovelya.accounting.domain.repository.CustomerCreditRepository;
import fr.kovelya.accounting.domain.repository.CustomerPaymentRepository;
import fr.kovelya.accounting.domain.repository.SalesInvoiceRepository;
import fr.kovelya.accounting.domain.shared.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class CustomerCreditApplicationServiceImpl implements CustomerCreditApplicationService {

    private final SalesInvoiceRepository salesInvoiceRepository;
    private final CustomerPaymentRepository customerPaymentRepository;
    private final CustomerCreditRepository customerCreditRepository;
    private final AccountRepository accountRepository;
    private final AccountingService accountingService;
    private final String receivableAccountCode;
    private final String customerAdvanceAccountCode;
    private final IdempotencyExecutor idempotencyExecutor;

    public CustomerCreditApplicationServiceImpl(
            SalesInvoiceRepository salesInvoiceRepository,
            CustomerPaymentRepository customerPaymentRepository,
            CustomerCreditRepository customerCreditRepository,
            AccountRepository accountRepository,
            AccountingService accountingService,
            String receivableAccountCode,
            String customerAdvanceAccountCode,
            IdempotencyExecutor idempotencyExecutor
    ) {
        this.salesInvoiceRepository = salesInvoiceRepository;
        this.customerPaymentRepository = customerPaymentRepository;
        this.customerCreditRepository = customerCreditRepository;
        this.accountRepository = accountRepository;
        this.accountingService = accountingService;
        this.receivableAccountCode = receivableAccountCode;
        this.customerAdvanceAccountCode = customerAdvanceAccountCode;
        this.idempotencyExecutor = idempotencyExecutor;
    }

    @Override
    public void applyCredit(UUID commandId, SalesInvoiceId invoiceId, Money amount, LocalDate date) {
        idempotencyExecutor.runVoid(
                commandId,
                () -> doApply(invoiceId, amount, date, commandId),
                () -> {}
        );
    }

    private void doApply(SalesInvoiceId invoiceId, Money amount, LocalDate date, UUID commandId) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        if (date == null) {
            throw new IllegalArgumentException("Date is required");
        }
        if (amount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        SalesInvoice invoice = salesInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        if (invoice.status() != InvoiceStatus.ISSUED && invoice.status() != InvoiceStatus.PARTIALLY_PAID) {
            throw new IllegalStateException("Only issued or partially paid invoices can be settled with credit");
        }

        if (!amount.currency().equals(invoice.total().currency())) {
            throw new IllegalArgumentException("Currency mismatch");
        }

        Money alreadyPaid = Money.zero(invoice.total().currency());
        for (CustomerPayment p : customerPaymentRepository.findByInvoice(invoiceId)) {
            alreadyPaid = alreadyPaid.add(p.amount());
        }

        Money remaining = invoice.total().subtract(alreadyPaid);
        if (remaining.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Invoice is already fully paid");
        }

        Money toApply = amount.amount().compareTo(remaining.amount()) > 0 ? remaining : amount;

        List<CustomerCredit> credits = customerCreditRepository.findOpenByCustomer(invoice.customerId());
        Money available = Money.zero(toApply.currency());
        for (CustomerCredit c : credits) {
            available = available.add(c.remaining());
        }
        if (available.amount().compareTo(toApply.amount()) < 0) {
            throw new IllegalStateException("Not enough customer credit available");
        }

        Money left = toApply;
        for (CustomerCredit c : credits) {
            if (left.amount().compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            Money canUse = c.remaining().amount().compareTo(left.amount()) > 0 ? left : c.remaining();
            customerCreditRepository.save(c.consume(canUse));
            left = left.subtract(canUse);
        }

        Account receivable = accountRepository.findByCode(receivableAccountCode)
                .orElseThrow(() -> new IllegalStateException("Receivable account not found: " + receivableAccountCode));

        Account advances = accountRepository.findByCode(customerAdvanceAccountCode)
                .orElseThrow(() -> new IllegalStateException("Customer advances account not found: " + customerAdvanceAccountCode));

        String reference = "CREDIT-APPLY-" + invoice.number() + "-" + commandId;

        AccountPosting debitAdv = new AccountPosting(advances.id(), toApply, LedgerEntry.Direction.DEBIT);
        AccountPosting creditAr = new AccountPosting(receivable.id(), toApply, LedgerEntry.Direction.CREDIT);

        accountingService.postJournalTransaction(
                JournalType.ADJUSTMENT,
                reference,
                "Apply customer credit to invoice " + invoice.number(),
                date,
                debitAdv,
                creditAr
        );

        CustomerPayment synthetic = CustomerPayment.create(invoice.id(), toApply, date, "CREDIT-4191");
        customerPaymentRepository.save(synthetic);

        SalesInvoice updated;
        if (toApply.amount().compareTo(remaining.amount()) == 0) {
            updated = invoice.markPaid();
        } else {
            updated = invoice.markPartiallyPaid();
        }
        salesInvoiceRepository.save(updated);
    }
}
