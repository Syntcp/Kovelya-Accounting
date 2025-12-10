package fr.kovelya.application;

import fr.kovelya.domain.model.AccountType;
import fr.kovelya.domain.model.Money;

public final class AccountBalanceView {

    private final String accountCode;
    private final String accountName;
    private final AccountType accountType;
    private final Money balance;

    public AccountBalanceView(String accountCode, String accountName, AccountType accountType, Money balance) {
        this.accountCode = accountCode;
        this.accountName = accountName;
        this.accountType = accountType;
        this.balance = balance;
    }

    public String accountCode() {
        return accountCode;
    }

    public String accountName() {
        return accountName;
    }

    public AccountType accountType() {
        return accountType;
    }

    public Money balance() {
        return balance;
    }
}
