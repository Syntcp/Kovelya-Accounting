package fr.kovelya.accounting.domain.repository;

import fr.kovelya.accounting.domain.ledger.JournalTransaction;
import fr.kovelya.accounting.domain.ledger.JournalType;
import fr.kovelya.accounting.domain.ledger.TransactionId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface JournalTransactionRepository {

    JournalTransaction save(JournalTransaction transaction);

    Optional<JournalTransaction> findById(TransactionId id);

    List<JournalTransaction> findAll();

    List<JournalTransaction> findByPeriod(Instant from, Instant to);

    List<JournalTransaction> findByJournal(JournalType journalType);

    List<JournalTransaction> findByJournalAndPeriod(JournalType journalType, Instant from, Instant to);
}
