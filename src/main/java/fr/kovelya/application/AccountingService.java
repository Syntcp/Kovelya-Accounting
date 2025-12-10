package fr.kovelya.application;

import fr.kovelya.domain.model.Account;

import java.util.List;

public interface AccountingService {

    Account openAccount(String name, String currencyCode);

    List<Account> listAccounts();
}
