package fr.kovelya.accounting.application.service.impl;

import fr.kovelya.accounting.application.report.BalanceSheetView;
import fr.kovelya.accounting.application.report.IncomeStatementView;
import fr.kovelya.accounting.application.service.AccountingService;
import fr.kovelya.accounting.application.service.FinancialStatementsService;
import fr.kovelya.accounting.domain.account.Account;
import fr.kovelya.accounting.domain.account.AccountType;
import fr.kovelya.accounting.domain.period.AccountingPeriod;
import fr.kovelya.accounting.domain.shared.Money;

import java.math.BigDecimal;
import java.util.Currency;

public final class FinancialStatementsServiceImpl implements FinancialStatementsService {

    private final AccountingService accountingService;

    public FinancialStatementsServiceImpl(AccountingService accountingService) {
        this.accountingService = accountingService;
    }

    @Override
    public IncomeStatementView getIncomeStatement(AccountingPeriod period) {
        Currency currency = null;
        BigDecimal revenueAmount = BigDecimal.ZERO;
        BigDecimal expenseAmount = BigDecimal.ZERO;

        for (Account account : accountingService.listAccounts()) {
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
    public BalanceSheetView getBalanceSheet(AccountingPeriod period) {
        Currency currency = null;
        BigDecimal assetsAmount = BigDecimal.ZERO;
        BigDecimal liabilitiesAmount = BigDecimal.ZERO;
        BigDecimal equityAmount = BigDecimal.ZERO;

        for (Account account : accountingService.listAccounts()) {
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
