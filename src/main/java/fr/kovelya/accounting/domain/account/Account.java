package fr.kovelya.accounting.domain.account;

import fr.kovelya.accounting.domain.ledger.LedgerId;

import java.util.Currency;
import java.util.Objects;

public final class Account {

    private final AccountId id;
    private final LedgerId ledgerId;
    private final String code;
    private final String name;
    private final AccountType type;
    private final Currency currency;
    private final boolean active;

    public Account(AccountId id, LedgerId ledgerId, String code, String name, AccountType type, Currency currency, boolean active) {
        if (ledgerId == null) {
            throw new IllegalArgumentException("Ledger is required");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Account code is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Account name is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("Account type is required");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Account currency is required");
        }
        this.id = id;
        this.ledgerId = ledgerId;
        this.code = code;
        this.name = name;
        this.type = type;
        this.currency = currency;
        this.active = active;
    }

    public static Account open(LedgerId ledgerId, String code, String name, AccountType type, Currency currency) {
        return new Account(AccountId.newId(), ledgerId, code, name, type, currency, true);
    }

    public AccountId id() {
        return id;
    }

    public LedgerId ledgerId() {
        return ledgerId;
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
        return new Account(id, ledgerId, code, name, type, currency, false);
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
