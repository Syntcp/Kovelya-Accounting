package fr.kovelya.accounting.application.service.impl;

import fr.kovelya.accounting.application.dto.AccountPosting;
import fr.kovelya.accounting.application.service.AccountingService;
import fr.kovelya.accounting.application.service.InvoicePostingService;
import fr.kovelya.accounting.domain.account.Account;
import fr.kovelya.accounting.domain.invoice.InvoiceLine;
import fr.kovelya.accounting.domain.invoice.InvoiceStatus;
import fr.kovelya.accounting.domain.invoice.SalesInvoice;
import fr.kovelya.accounting.domain.invoice.SalesInvoiceId;
import fr.kovelya.accounting.domain.ledger.JournalType;
import fr.kovelya.accounting.domain.ledger.LedgerEntry;
import fr.kovelya.accounting.domain.repository.AccountRepository;
import fr.kovelya.accounting.domain.repository.JournalTransactionRepository;
import fr.kovelya.accounting.domain.repository.SalesInvoiceRepository;
import fr.kovelya.accounting.domain.shared.Money;
import fr.kovelya.accounting.domain.tax.TaxCategory;
import fr.kovelya.accounting.domain.tax.VatRate;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class InvoicePostingServiceImpl implements InvoicePostingService {

    private final SalesInvoiceRepository salesInvoiceRepository;
    private final AccountRepository accountRepository;
    private final AccountingService accountingService;
    private final JournalTransactionRepository journalTransactionRepository;
    private final String receivableAccountCode;
    private final String revenueAccountCode;
    private final String vatAccountCode;
    private final VatRate vatRate;

    public InvoicePostingServiceImpl(SalesInvoiceRepository salesInvoiceRepository, AccountRepository accountRepository, AccountingService accountingService, JournalTransactionRepository journalTransactionRepository, String receivableAccountCode, String revenueAccountCode, String vatAccountCode, VatRate vatRate) {
        this.salesInvoiceRepository = salesInvoiceRepository;
        this.accountRepository = accountRepository;
        this.accountingService = accountingService;
        this.journalTransactionRepository = journalTransactionRepository;
        this.receivableAccountCode = receivableAccountCode;
        this.revenueAccountCode = revenueAccountCode;
        this.vatAccountCode = vatAccountCode;
        this.vatRate = vatRate;
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

        Account vatAccount = accountRepository.findByCode(vatAccountCode)
                .orElseThrow(() -> new IllegalStateException("VAT account not found: " + vatAccountCode));

        BigDecimal totalGrossAmount = BigDecimal.ZERO;
        BigDecimal totalNetAmount = BigDecimal.ZERO;
        BigDecimal totalVatAmount = BigDecimal.ZERO;

        for (InvoiceLine line : toPost.lines()) {
            BigDecimal grossAmount = line.amount().amount();
            BigDecimal lineRate;

            if (line.taxCategory() == TaxCategory.STANDARD) {
                lineRate = vatRate.value();
            } else {
                lineRate = BigDecimal.ZERO;
            }

            BigDecimal netAmount = grossAmount;
            BigDecimal vatAmount = BigDecimal.ZERO;

            if (lineRate.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal divisor = BigDecimal.ONE.add(lineRate);
                netAmount = grossAmount.divide(divisor, 2, RoundingMode.HALF_UP);
                vatAmount = grossAmount.subtract(netAmount);
            }

            totalGrossAmount = totalGrossAmount.add(grossAmount);
            totalNetAmount = totalNetAmount.add(netAmount);
            totalVatAmount = totalVatAmount.add(vatAmount);
        }

        Money gross = Money.of(totalGrossAmount, receivable.currency());
        Money net = Money.of(totalNetAmount, revenue.currency());
        Money vat = Money.of(totalVatAmount, vatAccount.currency());

        AccountPosting debitReceivable = new AccountPosting(receivable.id(), gross, LedgerEntry.Direction.DEBIT);
        AccountPosting creditRevenue = new AccountPosting(revenue.id(), net, LedgerEntry.Direction.CREDIT);
        AccountPosting creditVat = new AccountPosting(vatAccount.id(), vat, LedgerEntry.Direction.CREDIT);

        if (journalTransactionRepository.findByJournalAndReference(JournalType.SALES, toPost.number()).isPresent()) {
            return;
        }

        accountingService.postJournalTransaction(
                JournalType.SALES,
                toPost.number(),
                "Invoice " + toPost.number(),
                toPost.issueDate(),
                debitReceivable,
                creditRevenue,
                creditVat
        );

    }
}
