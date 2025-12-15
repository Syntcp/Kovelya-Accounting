package fr.kovelya.accounting.application.service.impl;

import fr.kovelya.accounting.application.dto.AccountPosting;
import fr.kovelya.accounting.application.service.AccountingService;
import fr.kovelya.accounting.application.service.PurchaseInvoicePostingService;
import fr.kovelya.accounting.domain.account.Account;
import fr.kovelya.accounting.domain.ledger.JournalType;
import fr.kovelya.accounting.domain.ledger.LedgerEntry;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoice;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceId;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceLine;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceStatus;
import fr.kovelya.accounting.domain.repository.AccountRepository;
import fr.kovelya.accounting.domain.repository.PurchaseInvoiceRepository;
import fr.kovelya.accounting.domain.shared.Money;
import fr.kovelya.accounting.domain.tax.TaxCategory;
import fr.kovelya.accounting.domain.tax.VatRate;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PurchaseInvoicePostingServiceImpl implements PurchaseInvoicePostingService {

    private final PurchaseInvoiceRepository purchaseInvoiceRepository;
    private final AccountRepository accountRepository;
    private final AccountingService accountingService;
    private final String payableAccountCode;
    private final String expenseAccountCode;
    private final String vatDeductibleAccountCode;
    private final VatRate vatRate;

    public PurchaseInvoicePostingServiceImpl(PurchaseInvoiceRepository purchaseInvoiceRepository, AccountRepository accountRepository, AccountingService accountingService, String payableAccountCode, String expenseAccountCode, String vatDeductibleAccountCode, VatRate vatRate) {
        this.purchaseInvoiceRepository = purchaseInvoiceRepository;
        this.accountRepository = accountRepository;
        this.accountingService = accountingService;
        this.payableAccountCode = payableAccountCode;
        this.expenseAccountCode = expenseAccountCode;
        this.vatDeductibleAccountCode = vatDeductibleAccountCode;
        this.vatRate = vatRate;
    }

    @Override
    public void postPurchaseInvoice(PurchaseInvoiceId invoiceId) {
        PurchaseInvoice invoice = purchaseInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase invoice not found"));

        PurchaseInvoice toPost = invoice;

        if (invoice.status() == PurchaseInvoiceStatus.DRAFT) {
            toPost = invoice.issue();
            purchaseInvoiceRepository.save(toPost);
        } else if (invoice.status() != PurchaseInvoiceStatus.ISSUED) {
            throw new IllegalStateException("Only draft or issued purchase invoices can be posted");
        }

        Account payable = accountRepository.findByCode(payableAccountCode)
                .orElseThrow(() -> new IllegalStateException("Payable account not found: " + payableAccountCode));

        Account expense = accountRepository.findByCode(expenseAccountCode)
                .orElseThrow(() -> new IllegalStateException("Expense account not found: " + expenseAccountCode));

        Account vatDeductible = accountRepository.findByCode(vatDeductibleAccountCode)
                .orElseThrow(() -> new IllegalStateException("VAT deductible account not found: " + vatDeductibleAccountCode));

        BigDecimal totalGrossAmount = BigDecimal.ZERO;
        BigDecimal totalNetAmount = BigDecimal.ZERO;
        BigDecimal totalVatAmount = BigDecimal.ZERO;

        for (PurchaseInvoiceLine line : toPost.lines()) {
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

        Money gross = Money.of(totalGrossAmount, payable.currency());
        Money net = Money.of(totalNetAmount, expense.currency());
        Money vat = Money.of(totalVatAmount, vatDeductible.currency());
        AccountPosting debitExpense = new AccountPosting(expense.id(), net, LedgerEntry.Direction.DEBIT);
        AccountPosting debitVat = new AccountPosting(vatDeductible.id(), vat, LedgerEntry.Direction.DEBIT);
        AccountPosting creditPayable = new AccountPosting(payable.id(), gross, LedgerEntry.Direction.CREDIT);

        String reference = toPost.number();
        String description = "Purchase invoice " + toPost.number();

        accountingService.postJournalTransaction(
                JournalType.PURCHASES,
                reference,
                description,
                toPost.issueDate(),
                debitExpense,
                debitVat,
                creditPayable
        );

    }
}
