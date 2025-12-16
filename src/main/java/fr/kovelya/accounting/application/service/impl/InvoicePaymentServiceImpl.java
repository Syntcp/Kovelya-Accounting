package fr.kovelya.accounting.application.service.impl;

import fr.kovelya.accounting.application.dto.AccountPosting;
import fr.kovelya.accounting.application.service.AccountingService;
import fr.kovelya.accounting.application.service.InvoicePaymentService;
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

public final class InvoicePaymentServiceImpl implements InvoicePaymentService {

    private final SalesInvoiceRepository salesInvoiceRepository;
    private final AccountRepository accountRepository;
    private final AccountingService accountingService;
    private final CustomerPaymentRepository customerPaymentRepository;
    private final CustomerCreditRepository customerCreditRepository;
    private final String receivableAccountCode;
    private final String customerAdvanceAccountCode;
    private final IdempotencyExecutor idempotencyExecutor;

    public InvoicePaymentServiceImpl(
            SalesInvoiceRepository salesInvoiceRepository,
            AccountRepository accountRepository,
            AccountingService accountingService,
            CustomerPaymentRepository customerPaymentRepository,
            CustomerCreditRepository customerCreditRepository,
            String receivableAccountCode,
            String customerAdvanceAccountCode,
            IdempotencyExecutor idempotencyExecutor
    ) {
        this.salesInvoiceRepository = salesInvoiceRepository;
        this.accountRepository = accountRepository;
        this.accountingService = accountingService;
        this.customerPaymentRepository = customerPaymentRepository;
        this.customerCreditRepository = customerCreditRepository;
        this.receivableAccountCode = receivableAccountCode;
        this.customerAdvanceAccountCode = customerAdvanceAccountCode;
        this.idempotencyExecutor = idempotencyExecutor;
    }

    @Override
    public void recordPayment(UUID commandId, SalesInvoiceId invoiceId, String bankAccountCode, Money amount, LocalDate paymentDate) {
        idempotencyExecutor.runVoid(
                commandId,
                () -> doRecordPayment(commandId, invoiceId, bankAccountCode, amount, paymentDate),
                () -> {}
        );
    }

    private void doRecordPayment(UUID commandId, SalesInvoiceId invoiceId, String bankAccountCode, Money amount, LocalDate paymentDate) {
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

        Account receivable = accountRepository.findByCode(receivableAccountCode)
                .orElseThrow(() -> new IllegalStateException("Receivable account not found: " + receivableAccountCode));

        Account bank = accountRepository.findByCode(bankAccountCode)
                .orElseThrow(() -> new IllegalStateException("Bank account not found: " + bankAccountCode));

        String reference = "BANK-PAY-" + invoice.number() + "-" + commandId;
        String description = "Payment of invoice " + invoice.number();

        AccountPosting debitBank = new AccountPosting(bank.id(), totalPaid, LedgerEntry.Direction.DEBIT);
        AccountPosting creditReceivable = new AccountPosting(receivable.id(), applied, LedgerEntry.Direction.CREDIT);

        if (excess.amount().compareTo(BigDecimal.ZERO) > 0) {
            Account advances = accountRepository.findByCode(customerAdvanceAccountCode)
                    .orElseThrow(() -> new IllegalStateException("Customer advances account not found: " + customerAdvanceAccountCode));

            AccountPosting creditAdvances = new AccountPosting(advances.id(), excess, LedgerEntry.Direction.CREDIT);

            accountingService.postJournalTransaction(
                    JournalType.BANK,
                    reference,
                    description,
                    paymentDate,
                    debitBank,
                    creditReceivable,
                    creditAdvances
            );

            if (customerCreditRepository.findBySourceCommandId(commandId).isEmpty()) {
                customerCreditRepository.save(CustomerCredit.create(invoice.customerId(), excess, commandId));
            }
        } else {
            accountingService.postJournalTransaction(
                    JournalType.BANK,
                    reference,
                    description,
                    paymentDate,
                    debitBank,
                    creditReceivable
            );
        }

        CustomerPayment payment = CustomerPayment.create(invoice.id(), applied, paymentDate, bankAccountCode);
        customerPaymentRepository.save(payment);

        SalesInvoice updated;
        if (applied.amount().compareTo(remaining.amount()) == 0) {
            updated = invoice.markPaid();
        } else {
            updated = invoice.markPartiallyPaid();
        }
        salesInvoiceRepository.save(updated);
    }
}
