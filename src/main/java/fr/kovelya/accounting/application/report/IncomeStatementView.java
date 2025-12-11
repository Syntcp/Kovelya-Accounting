package fr.kovelya.accounting.application.report;

import fr.kovelya.accounting.domain.period.AccountingPeriod;
import fr.kovelya.accounting.domain.shared.Money;

public final class IncomeStatementView {

    private final AccountingPeriod period;
    private final Money totalRevenue;
    private final Money totalExpenses;
    private final Money netIncome;

    public IncomeStatementView(AccountingPeriod period, Money totalRevenue, Money totalExpenses, Money netIncome) {
        this.period = period;
        this.totalRevenue = totalRevenue;
        this.totalExpenses = totalExpenses;
        this.netIncome = netIncome;
    }

    public AccountingPeriod period() {
        return period;
    }

    public Money totalRevenue() {
        return totalRevenue;
    }

    public Money totalExpenses() {
        return totalExpenses;
    }

    public Money netIncome() {
        return netIncome;
    }
}
