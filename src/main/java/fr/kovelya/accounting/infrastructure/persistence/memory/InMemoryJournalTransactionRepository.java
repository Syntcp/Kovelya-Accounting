package fr.kovelya.accounting.infrastructure.persistence.memory;

import fr.kovelya.accounting.domain.ledger.JournalTransaction;
import fr.kovelya.accounting.domain.ledger.JournalType;
import fr.kovelya.accounting.domain.ledger.TransactionId;
import fr.kovelya.accounting.domain.repository.JournalTransactionRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemoryJournalTransactionRepository implements JournalTransactionRepository {

    private final List<JournalTransaction> storage = new CopyOnWriteArrayList<>();

    @Override
    public JournalTransaction save(JournalTransaction transaction) {
        storage.add(transaction);
        return transaction;
    }

    @Override
    public Optional<JournalTransaction> findById(TransactionId id) {
        for (JournalTransaction transaction : storage) {
            if (transaction.id().equals(id)) {
                return Optional.of(transaction);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<JournalTransaction> findAll() {
        return new ArrayList<>(storage);
    }

    @Override
    public List<JournalTransaction> findByPeriod(Instant from, Instant to) {
        List<JournalTransaction> result = new ArrayList<>();
        for (JournalTransaction transaction : storage) {
            Instant ts = transaction.timestamp();
            if (!ts.isBefore(from) && !ts.isAfter(to)) {
                result.add(transaction);
            }
        }
        return result;
    }

    @Override
    public List<JournalTransaction> findByJournal(JournalType journalType) {
        List<JournalTransaction> result = new ArrayList<>();
        for (JournalTransaction transaction : storage) {
            if (transaction.journalType() == journalType) {
                result.add(transaction);
            }
        }
        return result;
    }

    @Override
    public List<JournalTransaction> findByJournalAndPeriod(JournalType journalType, Instant from, Instant to) {
        List<JournalTransaction> result = new ArrayList<>();
        for (JournalTransaction transaction : storage) {
            Instant ts = transaction.timestamp();
            if (transaction.journalType() == journalType
                    && !ts.isBefore(from)
                    && !ts.isAfter(to)) {
                result.add(transaction);
            }
        }
        return result;
    }
}
