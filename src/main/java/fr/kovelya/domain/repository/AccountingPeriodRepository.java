package fr.kovelya.domain.repository;

import fr.kovelya.domain.period.AccountingPeriod;
import fr.kovelya.domain.period.AccountingPeriodId;
import fr.kovelya.domain.period.PeriodStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccountingPeriodRepository {

    AccountingPeriod save(AccountingPeriod period);

    Optional<AccountingPeriod> findById(AccountingPeriodId id);

    List<AccountingPeriod> findAll();

    List<AccountingPeriod> findByStatus(PeriodStatus status);

    Optional<AccountingPeriod> findByDate(LocalDate date);
}
