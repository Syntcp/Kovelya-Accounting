package fr.kovelya.application;

import fr.kovelya.domain.model.Account;
import fr.kovelya.domain.repository.AccountRepository;

import java.util.Currency;
import java.util.List;

public final class AccountingServiceImpl implements AccountingService {

    private final AccountRepository accountRepository;

    public AccountingServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public Account openAccount(String name, String currencyCode) {
        Currency currency = Currency.getInstance(currencyCode);
        Account account = Account.open(name, currency);
        return accountRepository.save(account);
    }

    @Override
    public List<Account> listAccounts() {
        return accountRepository.findAll();
    }
}
