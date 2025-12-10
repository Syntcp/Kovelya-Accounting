package fr.kovelya.domain.ledger;

import fr.kovelya.domain.account.AccountId;
import fr.kovelya.domain.shared.Money;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class LedgerEntry {

    public enum Direction {
        DEBIT, CREDIT
    }

    private final String id;
    private final AccountId accountId;
    private final Money amount;
    private final Direction direction;
    private final String description;
    private final Instant timestamp;

    private LedgerEntry(String id, AccountId accountId, Money amount, Direction direction, String description, Instant timestamp) {
        this.id = id;
        this.accountId = accountId;
        this.amount = amount;
        this.direction = direction;
        this.description = description;
        this.timestamp = timestamp;
    }

    public static LedgerEntry create(AccountId accountId, Money amount, Direction direction, String description, Instant timestamp) {
        String id = UUID.randomUUID().toString();
        return new LedgerEntry(id, accountId, amount, direction, description, timestamp);
    }

    public String id() {
        return id;
    }

    public AccountId accountId() {
        return accountId;
    }

    public Money amount() {
        return amount;
    }

    public Direction direction() {
        return direction;
    }

    public String description() {
        return description;
    }

    public Instant timestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LedgerEntry that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
