package fr.kovelya.domain.repository;

import fr.kovelya.domain.model.AccountingPeriod;
import fr.kovelya.domain.model.AccountingPeriodId;
import fr.kovelya.domain.model.PeriodStatus;

import javax.swing.text.html.Option;
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
