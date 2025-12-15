package fr.kovelya.accounting.application.service.impl;

import fr.kovelya.accounting.application.report.BalanceSheetView;
import fr.kovelya.accounting.application.report.IncomeStatementView;
import fr.kovelya.accounting.application.service.AccountingService;
import fr.kovelya.accounting.application.service.FinancialStatementsService;
import fr.kovelya.accounting.domain.account.Account;
import fr.kovelya.accounting.domain.account.AccountId;
import fr.kovelya.accounting.domain.account.AccountType;
import fr.kovelya.accounting.domain.ledger.JournalTransaction;
import fr.kovelya.accounting.domain.ledger.LedgerEntry;
import fr.kovelya.accounting.domain.period.AccountingPeriod;
import fr.kovelya.accounting.domain.repository.JournalTransactionRepository;
import fr.kovelya.accounting.domain.shared.Money;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

public final class FinancialStatementsServiceImpl implements FinancialStatementsService {

    private final AccountingService accountingService;
    private final JournalTransactionRepository journalTransactionRepository;

    public FinancialStatementsServiceImpl(AccountingService accountingService, JournalTransactionRepository journalTransactionRepository) {
        this.accountingService = accountingService;
        this.journalTransactionRepository = journalTransactionRepository;
    }

    @Override
    public IncomeStatementView getIncomeStatement(AccountingPeriod period) {
        Currency currency = null;
        BigDecimal revenueAmount = BigDecimal.ZERO;
        BigDecimal expenseAmount = BigDecimal.ZERO;

        for (Account account : accountingService.listAccounts(period.ledgerId())) {
            Money balance = accountingService.getBalanceForPeriod(account.id(), period);
            if (balance.amount().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            if (currency == null) {
                currency = balance.currency();
            } else if (!balance.currency().equals(currency)) {
                throw new IllegalStateException("Multiple currencies not supported in income statement");
            }

            if (account.type() == AccountType.INCOME) {
                revenueAmount = revenueAmount.add(balance.amount());
            } else if (account.type() == AccountType.EXPENSE) {
                expenseAmount = expenseAmount.add(balance.amount());
            }
        }

        if (currency == null) {
            currency = Currency.getInstance("EUR");
        }

        Money totalRevenue = Money.of(revenueAmount, currency);
        Money totalExpenses = Money.of(expenseAmount, currency);
        Money netIncome = totalRevenue.subtract(totalExpenses);

        return new IncomeStatementView(period, totalRevenue, totalExpenses, netIncome);
    }

    @Override
    public IncomeStatementView getOperatingIncomeStatement(AccountingPeriod period) {
        Currency currency = null;
        Map<AccountId, BigDecimal> debit = new HashMap<>();
        Map<AccountId, BigDecimal> credit = new HashMap<>();

        for (JournalTransaction tx : journalTransactionRepository.findAll()) {
            if (!tx.getPeriodId().equals(period.id())) {
                continue;
            }
            String ref = tx.reference();
            if (ref != null && ref.startsWith("CLOSE-")) {
                continue;
            }

            for (LedgerEntry entry : tx.entries()) {
                Money amt = entry.amount();

                if (currency == null) {
                    currency = amt.currency();
                } else if (!currency.equals(amt.currency())) {
                    throw new IllegalStateException("Multiple currencies not supported in income statement");
                }

                if (entry.direction() == LedgerEntry.Direction.DEBIT) {
                    debit.merge(entry.accountId(), amt.amount(), BigDecimal::add);
                } else {
                    credit.merge(entry.accountId(), amt.amount(), BigDecimal::add);
                }
            }
        }

        if (currency == null) {
            currency = Currency.getInstance("EUR");
        }

        BigDecimal revenueAmount = BigDecimal.ZERO;
        BigDecimal expenseAmount = BigDecimal.ZERO;

        for (Account account : accountingService.listAccounts(period.ledgerId())) {
            BigDecimal d = debit.getOrDefault(account.id(), BigDecimal.ZERO);
            BigDecimal c = credit.getOrDefault(account.id(), BigDecimal.ZERO);

            if (account.type() == AccountType.INCOME) {
                revenueAmount = revenueAmount.add(c.subtract(d));
            } else if (account.type() == AccountType.EXPENSE) {
                expenseAmount = expenseAmount.add(d.subtract(c));
            }
        }

        Money totalRevenue = Money.of(revenueAmount, currency);
        Money totalExpenses = Money.of(expenseAmount, currency);
        Money netIncome = totalRevenue.subtract(totalExpenses);

        return new IncomeStatementView(period, totalRevenue, totalExpenses, netIncome);
    }


    @Override
    public BalanceSheetView getBalanceSheet(AccountingPeriod period) {
        Currency currency = null;
        BigDecimal assetsAmount = BigDecimal.ZERO;
        BigDecimal liabilitiesAmount = BigDecimal.ZERO;
        BigDecimal equityAmount = BigDecimal.ZERO;

        for (Account account : accountingService.listAccounts(period.ledgerId())) {
            Money balance = accountingService.getBalanceForPeriod(account.id(), period);
            if (balance.amount().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            if (currency == null) {
                currency = balance.currency();
            } else if (!balance.currency().equals(currency)) {
                throw new IllegalStateException("Multiple currencies not supported in balance sheet");
            }

            if (account.type() == AccountType.ASSET) {
                assetsAmount = assetsAmount.add(balance.amount());
            } else if (account.type() == AccountType.LIABILITY) {
                liabilitiesAmount = liabilitiesAmount.add(balance.amount());
            } else if (account.type() == AccountType.EQUITY) {
                equityAmount = equityAmount.add(balance.amount());
            }
        }

        if (currency == null) {
            currency = Currency.getInstance("EUR");
        }

        Money totalAssets = Money.of(assetsAmount, currency);
        Money totalLiabilities = Money.of(liabilitiesAmount, currency);
        Money totalEquity = Money.of(equityAmount, currency);
        Money derivedEquity = totalAssets.subtract(totalLiabilities);

        return new BalanceSheetView(period, totalAssets, totalLiabilities, totalEquity, derivedEquity);
    }

}
