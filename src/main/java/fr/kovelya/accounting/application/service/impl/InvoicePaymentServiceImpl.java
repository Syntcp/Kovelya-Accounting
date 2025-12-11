package fr.kovelya.accounting.application.service.impl;

import fr.kovelya.accounting.application.service.AccountingService;
import fr.kovelya.accounting.application.service.InvoicePaymentService;
import fr.kovelya.accounting.domain.account.Account;
import fr.kovelya.accounting.domain.invoice.InvoiceStatus;
import fr.kovelya.accounting.domain.invoice.SalesInvoice;
import fr.kovelya.accounting.domain.invoice.SalesInvoiceId;
import fr.kovelya.accounting.domain.ledger.JournalType;
import fr.kovelya.accounting.domain.repository.AccountRepository;
import fr.kovelya.accounting.domain.repository.SalesInvoiceRepository;

public final class InvoicePaymentServiceImpl implements InvoicePaymentService {

    private final SalesInvoiceRepository salesInvoiceRepository;
    private final AccountRepository accountRepository;
    private final AccountingService accountingService;
    private final String receivableAccountCode;

    public InvoicePaymentServiceImpl(SalesInvoiceRepository salesInvoiceRepository, AccountRepository accountRepository, AccountingService accountingService, String receivableAccountCode) {
        this.salesInvoiceRepository = salesInvoiceRepository;
        this.accountRepository = accountRepository;
        this.accountingService = accountingService;
        this.receivableAccountCode = receivableAccountCode;
    }

    @Override
    public void recordPayment(SalesInvoiceId invoiceId, String bankAccountCode) {
        SalesInvoice invoice = salesInvoiceRepository.findById((invoiceId))
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        if (invoice.status() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Invoice is already paid");
        }

        if(invoice.status() != InvoiceStatus.ISSUED) {
            throw new IllegalStateException("Only issued invoices can be paid");
        }

        Account receivable = accountRepository.findByCode(receivableAccountCode)
                .orElseThrow(() -> new IllegalStateException("Receivable account not found: " + receivableAccountCode));

        Account bank = accountRepository.findByCode(bankAccountCode)
                .orElseThrow(() -> new IllegalStateException("Bank account not found: " + bankAccountCode));

        accountingService.transfer(
                bank.id(),
                receivable.id(),
                invoice.total(),
                JournalType.BANK,
                "Payment of invoice " + invoice.number()
        );

        SalesInvoice paid = invoice.markPaid();
        salesInvoiceRepository.save(paid);
    }
}
