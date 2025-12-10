package fr.kovelya.infrastructure.persistence.memory;

import fr.kovelya.domain.account.Account;
import fr.kovelya.domain.account.AccountId;
import fr.kovelya.domain.repository.AccountRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryAccountRepository implements AccountRepository {

    private final ConcurrentHashMap<String, Account> storage = new ConcurrentHashMap<>();

    @Override
    public Account save(Account account) {
        storage.put(account.id().value(), account);
        return account;
    }

    @Override
    public Optional<Account> findById(AccountId id) {
        return Optional.ofNullable(storage.get(id.value()));
    }

    @Override
    public List<Account> findAll() {
        return new ArrayList<>(storage.values());
    }
}
