package fr.kovelya.application;

import fr.kovelya.domain.model.Account;
import fr.kovelya.domain.model.AccountId;
import fr.kovelya.domain.model.Money;

import java.util.List;

public interface AccountingService {

    Account openAccount(String name, String currencyCode);

    void transfer(AccountId from, AccountId to, Money amount, String description);

    Money getBalance(AccountId accountId);

    List<Account> listAccounts();
}
