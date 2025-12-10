package fr.kovelya.accounting.domain.repository;

import fr.kovelya.accounting.domain.account.AccountId;
import fr.kovelya.accounting.domain.ledger.LedgerEntry;

import java.time.Instant;
import java.util.List;

public interface LedgerEntryRepository {

    LedgerEntry save(LedgerEntry entry);

    List<LedgerEntry> findByAccount(AccountId accountId);

    List<LedgerEntry> findByAccountAndPeriod(AccountId accountId, Instant from, Instant to);
}
