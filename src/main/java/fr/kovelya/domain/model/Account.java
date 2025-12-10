package fr.kovelya.domain.model;

import java.util.Currency;
import java.util.Objects;

public final class Account {

    private final AccountId id;
    private final String name;
    private final Currency currency;
    private final boolean active;

    public Account(AccountId id, String name, Currency currency, boolean active) {
        this.id = id;
        this.name = name;
        this.currency = currency;
        this.active = active;
    }

    public static Account open(String name, Currency currency) {
        return new Account(AccountId.newId(), name, currency, true);
    }

    public AccountId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Currency currency() {
        return currency;
    }

    public boolean isActive() {
        return active;
    }

    public Account deactivate() {
        return new Account(this.id, this.name, this.currency, false);
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
