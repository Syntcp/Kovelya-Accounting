package fr.kovelya.infrastructure.persistence.memory;

import fr.kovelya.domain.account.AccountId;
import fr.kovelya.domain.ledger.LedgerEntry;
import fr.kovelya.domain.repository.LedgerEntryRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemoryLedgerEntryRepository implements LedgerEntryRepository {

    private final List<LedgerEntry> storage = new CopyOnWriteArrayList<>();

    @Override
    public LedgerEntry save(LedgerEntry entry) {
        storage.add(entry);
        return entry;
    }

    @Override
    public List<LedgerEntry> findByAccount(AccountId accountId) {
        List<LedgerEntry> result = new ArrayList<>();
        for (LedgerEntry entry : storage) {
            if (entry.accountId().equals(accountId)) {
                result.add(entry);
            }
        }
        return result;
    }

    @Override
    public List<LedgerEntry> findByAccountAndPeriod(AccountId accountId, Instant from, Instant to) {
        List<LedgerEntry> result = new ArrayList<>();
        for (LedgerEntry entry : storage) {
            if (entry.accountId().equals(accountId)
                    && !entry.timestamp().isBefore(from)
                    && !entry.timestamp().isAfter(to)) {
                result.add(entry);
            }
        }
        return result;
    }
}
