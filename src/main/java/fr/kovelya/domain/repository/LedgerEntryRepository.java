package fr.kovelya.domain.repository;

import fr.kovelya.domain.model.AccountId;
import fr.kovelya.domain.model.LedgerEntry;

import java.time.Instant;
import java.util.List;

public interface LedgerEntryRepository {

    LedgerEntry save(LedgerEntry entry);

    List<LedgerEntry> findByAccount(AccountId accountId);

    List<LedgerEntry> findByAccountAndPeriod(AccountId accountId, Instant from, Instant to);
}
