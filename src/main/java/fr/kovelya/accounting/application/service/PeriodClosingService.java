package fr.kovelya.accounting.application.service;

import fr.kovelya.accounting.domain.account.AccountId;
import fr.kovelya.accounting.domain.period.AccountingPeriod;

public interface PeriodClosingService {

    void closePeriod(AccountingPeriod period, AccountId retainedEarningsAccountId);
}
