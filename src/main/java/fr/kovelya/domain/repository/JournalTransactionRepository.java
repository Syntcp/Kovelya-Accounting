package fr.kovelya.domain.repository;

import fr.kovelya.domain.model.JournalTransaction;
import fr.kovelya.domain.model.TransactionId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface JournalTransactionRepository {

    JournalTransaction save(JournalTransaction transaction);

    Optional<JournalTransaction> findById(TransactionId id);

    List<JournalTransaction> findAll();

    List<JournalTransaction> findByPeriod(Instant from, Instant to);
}
