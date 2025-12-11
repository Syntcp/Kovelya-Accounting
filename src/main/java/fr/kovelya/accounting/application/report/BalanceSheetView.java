package fr.kovelya.accounting.application.report;

import fr.kovelya.accounting.domain.period.AccountingPeriod;
import fr.kovelya.accounting.domain.shared.Money;

public final class BalanceSheetView {

    private final AccountingPeriod period;
    private final Money totalAssets;
    private final Money totalLiabilities;
    private final Money derivedEquity;

    public BalanceSheetView(AccountingPeriod period, Money totalAssets, Money totalLiabilities, Money derivedEquity) {
        this.period = period;
        this.totalAssets = totalAssets;
        this.totalLiabilities = totalLiabilities;
        this.derivedEquity = derivedEquity;
    }

    public AccountingPeriod period() {
        return period;
    }

    public Money totalAssets() {
        return totalAssets;
    }

    public Money totalLiabilities() {
        return totalLiabilities;
    }

    public Money derivedEquity() {
        return derivedEquity;
    }
}
