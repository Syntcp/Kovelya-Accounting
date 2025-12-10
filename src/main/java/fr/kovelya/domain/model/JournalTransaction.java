package fr.kovelya.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

public final class JournalTransaction {

    private final TransactionId id;
    private final String reference;
    private final String description;
    private final Instant timestamp;
    private final List<LedgerEntry> entries;

    public JournalTransaction(TransactionId id, String reference, String description, Instant timestamp, List<LedgerEntry> entries) {
        this.id = id;
        this.reference = reference;
        this.description = description;
        this.timestamp = timestamp;
        this.entries = entries;
    }

    public static JournalTransaction create(String reference, String description, Instant timestamp, List<LedgerEntry> entries) {
        if (entries == null || entries.size() < 2) {
            throw new IllegalArgumentException("Transaction must have at least two entries");
        }

        Currency currency = entries.get(0).amount().currency();
        BigDecimal total = BigDecimal.ZERO;

        for(LedgerEntry entry : entries) {
            if (!entry.amount().currency().equals(currency)) {
                throw new IllegalArgumentException("All entries must have the same currency");
            }
            BigDecimal signedAmount = entry.direction() == LedgerEntry.Direction.DEBIT
                    ? entry.amount().amount()
                    :  entry.amount().amount().negate();
            total = total.add(signedAmount);
        }

        if(total.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("Transaction is not balanced");
        }

        List<LedgerEntry> copy = List.copyOf(entries);
        return new JournalTransaction(TransactionId.newId(), reference, description, timestamp, copy);
    }

    public TransactionId id() {
        return id;
    }

    public String reference() {
        return reference;
    }

    public String description() {
        return description;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public List<LedgerEntry> entries() {
        return entries;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JournalTransaction that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
