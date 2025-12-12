package fr.kovelya.accounting.application.report;

import fr.kovelya.accounting.domain.period.AccountingPeriod;
import fr.kovelya.accounting.domain.shared.Money;

public final class BalanceSheetView {

    private final AccountingPeriod period;
    private final Money totalAssets;
    private final Money totalLiabilities;
    private final Money totalEquity;
    private final Money derivedEquity;

    public BalanceSheetView(AccountingPeriod period, Money totalAssets, Money totalLiabilities, Money totalEquity, Money derivedEquity) {
        this.period = period;
        this.totalAssets = totalAssets;
        this.totalLiabilities = totalLiabilities;
        this.totalEquity = totalEquity;
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

    public Money totalEquity() { return totalEquity; }

    public Money derivedEquity() {
        return derivedEquity;
    }
}
