package fr.kovelya.accounting.application;

import fr.kovelya.accounting.domain.account.Account;
import fr.kovelya.accounting.domain.invoice.InvoiceStatus;
import fr.kovelya.accounting.domain.invoice.SalesInvoice;
import fr.kovelya.accounting.domain.invoice.SalesInvoiceId;
import fr.kovelya.accounting.domain.ledger.JournalType;
import fr.kovelya.accounting.domain.repository.AccountRepository;
import fr.kovelya.accounting.domain.repository.SalesInvoiceRepository;

public final class InvoicePostingServiceImpl implements InvoicePostingService{

    private final SalesInvoiceRepository salesInvoiceRepository;
    private final AccountRepository accountRepository;
    private final AccountingService accountingService;
    private final String receivableAccountCode;
    private final String revenueAccountCode;

    public InvoicePostingServiceImpl(SalesInvoiceRepository salesInvoiceRepository, AccountRepository accountRepository, AccountingService accountingService, String receivableAccountCode, String revenueAccountCode) {
        this.salesInvoiceRepository = salesInvoiceRepository;
        this.accountRepository = accountRepository;
        this.accountingService = accountingService;
        this.receivableAccountCode = receivableAccountCode;
        this.revenueAccountCode = revenueAccountCode;
    }

    @Override
    public void postInvoice(SalesInvoiceId invoiceId) {
        SalesInvoice invoice = salesInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        SalesInvoice toPost = invoice;

        if (invoice.status() == InvoiceStatus.DRAFT) {
            toPost = invoice.issue();
            salesInvoiceRepository.save(toPost);
        } else if (invoice.status() != InvoiceStatus.ISSUED) {
            throw new IllegalStateException("Only draft or issued invoices can be posted");
        }

        Account receivable = accountRepository.findByCode(receivableAccountCode)
                .orElseThrow(() -> new IllegalStateException("Receivable account not found: " + receivableAccountCode));

        Account revenue = accountRepository.findByCode(revenueAccountCode)
                .orElseThrow(() -> new IllegalStateException("Revenue account not found: " + revenueAccountCode));

        accountingService.transfer(
                receivable.id(),
                revenue.id(),
                toPost.total(),
                JournalType.SALES,
                "Invoice " + toPost.number()
        );
    }
}
