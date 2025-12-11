package fr.kovelya.accounting.domain.repository;

import fr.kovelya.accounting.domain.account.Account;
import fr.kovelya.accounting.domain.account.AccountId;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {

    Account save(Account account);

    Optional<Account> findById(AccountId id);

    Optional<Account> findByCode(String code);

    List<Account> findAll();
}
