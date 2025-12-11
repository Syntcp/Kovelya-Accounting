package fr.kovelya.accounting.application.service;

import fr.kovelya.accounting.application.report.AccountBalanceView;
import fr.kovelya.accounting.application.dto.AccountPosting;
import fr.kovelya.accounting.domain.account.Account;
import fr.kovelya.accounting.domain.account.AccountId;
import fr.kovelya.accounting.domain.account.AccountType;
import fr.kovelya.accounting.domain.period.AccountingPeriod;
import fr.kovelya.accounting.domain.ledger.JournalTransaction;
import fr.kovelya.accounting.domain.ledger.JournalType;
import fr.kovelya.accounting.domain.shared.Money;

import java.time.LocalDate;
import java.util.List;

public interface AccountingService {

    Account openAccount(String code, String name, String currencyCode, AccountType type);

    void transfer(AccountId from, AccountId to, Money amount, JournalType journalType, String description);

    void postJournalTransaction(JournalType journalType, String description, AccountPosting... postings);

    Money getBalance(AccountId accountId);

    Money getBalanceForPeriod(AccountId accountId, AccountingPeriod period);

    List<Account> listAccounts();

    List<JournalTransaction> listTransactions();

    AccountingPeriod createPeriod(String name, LocalDate startDate, LocalDate endDate);

    List<AccountingPeriod> listPeriods();

    List<AccountBalanceView> getTrialBalance(AccountingPeriod period);
}
