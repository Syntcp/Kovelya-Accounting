package fr.kovelya.infrastructure.persistence.memory;

import fr.kovelya.domain.period.AccountingPeriod;
import fr.kovelya.domain.period.AccountingPeriodId;
import fr.kovelya.domain.period.PeriodStatus;
import fr.kovelya.domain.repository.AccountingPeriodRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryAccountingPeriodRepository implements AccountingPeriodRepository {

    private final ConcurrentHashMap<String, AccountingPeriod> storage = new ConcurrentHashMap<>();

    @Override
    public AccountingPeriod save(AccountingPeriod period) {
        storage.put(period.id().value(), period);
        return period;
    }

    @Override
    public Optional<AccountingPeriod> findById(AccountingPeriodId id) {
        return Optional.ofNullable(storage.get(id.value()));
    }

    @Override
    public List<AccountingPeriod> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public List<AccountingPeriod> findByStatus(PeriodStatus status) {
        List<AccountingPeriod> result = new ArrayList<>();
        for (AccountingPeriod period : storage.values()) {
            if (period.status() == status) {
                result.add(period);
            }
        }
        return result;
    }

    @Override
    public Optional<AccountingPeriod> findByDate(LocalDate date) {
        for (AccountingPeriod period : storage.values()) {
            if (!date.isBefore(period.startDate()) && !date.isAfter(period.endDate())) {
                return Optional.of(period);
            }
        }
        return Optional.empty();
    }
}
