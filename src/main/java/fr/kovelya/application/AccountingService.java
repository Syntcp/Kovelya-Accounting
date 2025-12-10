package fr.kovelya.application;

import fr.kovelya.domain.model.*;

import java.util.List;

public interface AccountingService {

    Account openAccount(String code, String name, String currencyCode, AccountType type);

    void transfer(AccountId from, AccountId to, Money amount, JournalType journalType, String description);

    Money getBalance(AccountId accountId);

    List<Account> listAccounts();

    List<JournalTransaction> listTransactions();
}
