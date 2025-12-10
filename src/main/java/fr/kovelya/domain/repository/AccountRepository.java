package fr.kovelya.domain.repository;

import fr.kovelya.domain.account.Account;
import fr.kovelya.domain.account.AccountId;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {

    Account save(Account account);

    Optional<Account> findById(AccountId id);

    List<Account> findAll();
}
