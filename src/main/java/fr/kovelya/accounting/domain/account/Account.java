package fr.kovelya.accounting.domain.account;

import java.util.Currency;
import java.util.Objects;

public final class Account {

    private final AccountId id;
    private final String code;
    private final String name;
    private final AccountType type;
    private final Currency currency;
    private final boolean active;

    public Account(AccountId id, String code, String name, AccountType type, Currency currency, boolean active) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Account code is required");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Account name is required");
        }
        this.id = id;
        this.code = code;
        this.name = name;
        this.type = type;
        this.currency = currency;
        this.active = active;
    }

    public static Account open(String code, String name, AccountType type, Currency currency) {
        return new Account(AccountId.newId(), code, name, type, currency, true);
    }

    public AccountId id() {
        return id;
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }

    public AccountType type() {
        return type;
    }

    public Currency currency() {
        return currency;
    }

    public boolean isActive() {
        return active;
    }

    public Account deactivate() {
        return new Account(this.id, this.code, this.name, this.type, this.currency, false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if(!(o instanceof Account account)) return false;
        return Objects.equals(id, account.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
