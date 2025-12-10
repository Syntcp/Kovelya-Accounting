package fr.kovelya.application;

import fr.kovelya.domain.model.Account;
import fr.kovelya.domain.model.AccountId;
import fr.kovelya.domain.model.AccountType;
import fr.kovelya.domain.model.AccountingPeriod;
import fr.kovelya.domain.model.JournalTransaction;
import fr.kovelya.domain.model.JournalType;
import fr.kovelya.domain.model.Money;

import java.time.LocalDate;
import java.util.List;

public interface AccountingService {

    Account openAccount(String code, String name, String currencyCode, AccountType type);

    void transfer(AccountId from, AccountId to, Money amount, JournalType journalType, String description);

    Money getBalance(AccountId accountId);

    Money getBalanceForPeriod(AccountId accountId, AccountingPeriod period);

    List<Account> listAccounts();

    List<JournalTransaction> listTransactions();

    AccountingPeriod createPeriod(String name, LocalDate startDate, LocalDate endDate);

    List<AccountingPeriod> listPeriods();

    List<AccountBalanceView> getTrialBalance(AccountingPeriod period);
}
