package fr.kovelya.accounting.application.service.impl;

import fr.kovelya.accounting.application.dto.AccountPosting;
import fr.kovelya.accounting.application.service.AccountingService;
import fr.kovelya.accounting.application.service.SalesCreditNoteService;
import fr.kovelya.accounting.domain.account.Account;
import fr.kovelya.accounting.domain.invoice.InvoiceLine;
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
import java.time.LocalDate;

public final class SalesCreditNoteServiceImpl implements SalesCreditNoteService {

    private final SalesInvoiceRepository salesInvoiceRepository;
    private final AccountRepository accountRepository;
    private final AccountingService accountingService;
    private final JournalTransactionRepository journalTransactionRepository;
    private final String receivableAccountCode;
    private final String revenueAccountCode;
    private final String vatCollectedAccountCode;
    private final VatRate vatRate;

    public SalesCreditNoteServiceImpl(
            SalesInvoiceRepository salesInvoiceRepository,
            AccountRepository accountRepository,
            AccountingService accountingService,
            JournalTransactionRepository journalTransactionRepository,
            String receivableAccountCode,
            String revenueAccountCode,
            String vatCollectedAccountCode,
            VatRate vatRate
    ) {
        this.salesInvoiceRepository = salesInvoiceRepository;
        this.accountRepository = accountRepository;
        this.accountingService = accountingService;
        this.journalTransactionRepository = journalTransactionRepository;
        this.receivableAccountCode = receivableAccountCode;
        this.revenueAccountCode = revenueAccountCode;
        this.vatCollectedAccountCode = vatCollectedAccountCode;
        this.vatRate = vatRate;
    }

    @Override
    public void issueFullCreditNote(SalesInvoiceId originalInvoiceId, String creditNoteNumber, java.time.LocalDate issueDate) {
        if (creditNoteNumber == null || creditNoteNumber.isBlank()) {
            throw new IllegalArgumentException("Credit note number is required");
        }
        if (issueDate == null) {
            throw new IllegalArgumentException("Issue date is required");
        }

        SalesInvoice invoice = salesInvoiceRepository.findById(originalInvoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Sales invoice not found"));

        if (journalTransactionRepository.findByJournalAndReference(JournalType.SALES, invoice.number()).isEmpty()) {
            throw new IllegalStateException("Cannot credit a non-posted invoice: " + invoice.number());
        }

        if (journalTransactionRepository.findByJournalAndReference(JournalType.SALES, creditNoteNumber).isPresent()) {
            return;
        }

        Account receivable = accountRepository.findByCode(receivableAccountCode)
                .orElseThrow(() -> new IllegalStateException("Receivable account not found: " + receivableAccountCode));

        Account revenue = accountRepository.findByCode(revenueAccountCode)
                .orElseThrow(() -> new IllegalStateException("Revenue account not found: " + revenueAccountCode));

        Account vatCollected = accountRepository.findByCode(vatCollectedAccountCode)
                .orElseThrow(() -> new IllegalStateException("VAT collected account not found: " + vatCollectedAccountCode));

        BigDecimal totalGrossAmount = BigDecimal.ZERO;
        BigDecimal totalNetAmount = BigDecimal.ZERO;
        BigDecimal totalVatAmount = BigDecimal.ZERO;

        for (InvoiceLine line : invoice.lines()) {
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
        Money vat = Money.of(totalVatAmount, vatCollected.currency());

        AccountPosting debitRevenue = new AccountPosting(revenue.id(), net, LedgerEntry.Direction.DEBIT);
        AccountPosting debitVat = new AccountPosting(vatCollected.id(), vat, LedgerEntry.Direction.DEBIT);
        AccountPosting creditReceivable = new AccountPosting(receivable.id(), gross, LedgerEntry.Direction.CREDIT);

        accountingService.postJournalTransaction(
                JournalType.SALES,
                creditNoteNumber,
                "Credit note " + creditNoteNumber + " for invoice " + invoice.number(),
                issueDate,
                debitRevenue,
                debitVat,
                creditReceivable
        );
    }

    @Override
    public void issuePartialCreditNote(SalesInvoiceId originalInvoiceId, String creditNoteNumber, LocalDate issueDate, Money grossAmountToCredit) {
        if (creditNoteNumber == null || creditNoteNumber.isBlank()) {
            throw new IllegalArgumentException("Credit note number is required");
        }
        if (issueDate == null) {
            throw new IllegalArgumentException("Issue date is required");
        }
        if (grossAmountToCredit == null) {
            throw new IllegalArgumentException("Gross amount to credit is required");
        }
        if (grossAmountToCredit.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Gross amount to credit must be positive");
        }

        SalesInvoice invoice = salesInvoiceRepository.findById(originalInvoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Sales invoice not found"));

        if (journalTransactionRepository.findByJournalAndReference(JournalType.SALES, invoice.number()).isEmpty()) {
            throw new IllegalStateException("Cannot credit a non-posted invoice: " + invoice.number());
        }

        if (journalTransactionRepository.findByJournalAndReference(JournalType.SALES, creditNoteNumber).isPresent()) {
            return;
        }

        Account receivable = accountRepository.findByCode(receivableAccountCode)
                .orElseThrow(() -> new IllegalStateException("Receivable account not found: " + receivableAccountCode));

        Account revenue = accountRepository.findByCode(revenueAccountCode)
                .orElseThrow(() -> new IllegalStateException("Revenue account not found: " + revenueAccountCode));

        Account vatCollected = accountRepository.findByCode(vatCollectedAccountCode)
                .orElseThrow(() -> new IllegalStateException("VAT collected account not found: " + vatCollectedAccountCode));

        if (!grossAmountToCredit.currency().equals(receivable.currency())) {
            throw new IllegalArgumentException("Currency mismatch for credit note");
        }

        BigDecimal totalGrossAmount = BigDecimal.ZERO;
        BigDecimal totalNetAmount = BigDecimal.ZERO;
        BigDecimal totalVatAmount = BigDecimal.ZERO;

        for (InvoiceLine line : invoice.lines()) {
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

        if (totalGrossAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Invoice total must be positive");
        }

        BigDecimal grossToCredit = grossAmountToCredit.amount();
        if (grossToCredit.compareTo(totalGrossAmount) > 0) {
            throw new IllegalArgumentException("Cannot credit more than invoice total");
        }

        BigDecimal ratioNet = totalNetAmount.divide(totalGrossAmount, 12, RoundingMode.HALF_UP);
        BigDecimal netToCredit = grossToCredit.multiply(ratioNet).setScale(2, RoundingMode.HALF_UP);
        BigDecimal vatToCredit = grossToCredit.subtract(netToCredit);

        Money gross = Money.of(grossToCredit, receivable.currency());
        Money net = Money.of(netToCredit, revenue.currency());
        Money vat = Money.of(vatToCredit, vatCollected.currency());

        AccountPosting debitRevenue = new AccountPosting(revenue.id(), net, LedgerEntry.Direction.DEBIT);
        AccountPosting debitVat = new AccountPosting(vatCollected.id(), vat, LedgerEntry.Direction.DEBIT);
        AccountPosting creditReceivable = new AccountPosting(receivable.id(), gross, LedgerEntry.Direction.CREDIT);

        accountingService.postJournalTransaction(
                JournalType.SALES,
                creditNoteNumber,
                "Partial credit note " + creditNoteNumber + " for invoice " + invoice.number(),
                issueDate,
                debitRevenue,
                debitVat,
                creditReceivable
        );
    }
}
