package fr.kovelya.accounting.domain.ledger;

import fr.kovelya.accounting.domain.period.AccountingPeriodId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

public final class JournalTransaction {

    private final TransactionId id;
    private final JournalType journalType;
    private final String reference;
    private final String description;
    private final Instant timestamp;
    private final LocalDate transactionDate;
    private final AccountingPeriodId periodId;
    private final List<LedgerEntry> entries;


    private JournalTransaction(TransactionId id, JournalType journalType, String reference, String description, Instant timestamp, LocalDate transactionDate, AccountingPeriodId periodId, List<LedgerEntry> entries) {
        this.id = id;
        this.journalType = journalType;
        this.reference = reference;
        this.description = description;
        this.timestamp = timestamp;
        this.transactionDate = transactionDate;
        this.periodId = periodId;
        this.entries = entries;
    }

    public static JournalTransaction create(JournalType journalType, String reference, String description, Instant timestamp, LocalDate transactionDate, AccountingPeriodId periodId, List<LedgerEntry> entries) {
        if (journalType == null) {
            throw new IllegalArgumentException("Journal type is required");
        }
        if (entries == null || entries.size() < 2) {
            throw new IllegalArgumentException("Transaction must have at least two entries");
        }
        if (transactionDate == null) {
            throw new IllegalArgumentException("Transaction date is required");
        }
        if (periodId == null) {
            throw new IllegalArgumentException("Accounting period is required");
        }

        Currency currency = entries.get(0).amount().currency();
        BigDecimal total = BigDecimal.ZERO;

        for (LedgerEntry entry : entries) {
            if (!entry.amount().currency().equals(currency)) {
                throw new IllegalArgumentException("All entries must have the same currency");
            }
            BigDecimal signedAmount = entry.direction() == LedgerEntry.Direction.DEBIT
                    ? entry.amount().amount()
                    : entry.amount().amount().negate();
            total = total.add(signedAmount);
        }

        if (total.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("Transaction is not balanced");
        }

        List<LedgerEntry> copy = List.copyOf(entries);
        return new JournalTransaction(
                TransactionId.newId(),
                journalType,
                reference,
                description,
                timestamp,
                transactionDate,
                periodId,
                copy
        );
    }


    public TransactionId id() {
        return id;
    }

    public JournalType journalType() {
        return journalType;
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

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public AccountingPeriodId getPeriodId() {
        return periodId;
    }

    public List<LedgerEntry> entries() {
        return new ArrayList<>(entries);
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
