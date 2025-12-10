package fr.kovelya.accounting.domain.repository;

import fr.kovelya.accounting.domain.period.AccountingPeriod;
import fr.kovelya.accounting.domain.period.AccountingPeriodId;
import fr.kovelya.accounting.domain.period.PeriodStatus;

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
