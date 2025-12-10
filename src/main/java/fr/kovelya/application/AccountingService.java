package fr.kovelya.application;

import fr.kovelya.domain.account.Account;
import fr.kovelya.domain.account.AccountId;
import fr.kovelya.domain.account.AccountType;
import fr.kovelya.domain.period.AccountingPeriod;
import fr.kovelya.domain.ledger.JournalTransaction;
import fr.kovelya.domain.ledger.JournalType;
import fr.kovelya.domain.shared.Money;

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
