package fr.kovelya.application;

import fr.kovelya.domain.model.Account;
import fr.kovelya.domain.model.AccountId;
import fr.kovelya.domain.model.LedgerEntry;
import fr.kovelya.domain.model.Money;
import fr.kovelya.domain.repository.AccountRepository;
import fr.kovelya.domain.repository.LedgerEntryRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;

public final class AccountingServiceImpl implements AccountingService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public AccountingServiceImpl(AccountRepository accountRepository, LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Override
    public Account openAccount(String name, String currencyCode) {
        Currency currency = Currency.getInstance(currencyCode);
        Account account = Account.open(name, currency);
        return accountRepository.save(account);
    }

    @Override
    public void transfer(AccountId from, AccountId to, Money amount, String description) {
        if(amount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        Account fromAccount = accountRepository.findById(from)
                .orElseThrow(() -> new IllegalArgumentException("Source account not found"));
        Account toAccount = accountRepository.findById(to)
                .orElseThrow(() -> new IllegalArgumentException("Destination account not found"));

        if(!fromAccount.currency().equals(toAccount.currency())) {
            throw new IllegalArgumentException("Currency mismatch between accounts");
        }

        if(!fromAccount.currency().equals(amount.currency())) {
            throw new IllegalArgumentException("Currency mismatch between account and amount");
        }

        Instant now = Instant.now();

        LedgerEntry debit = LedgerEntry.create(
                from,
                amount,
                LedgerEntry.Direction.DEBIT,
                description,
                now
        );

        LedgerEntry credit = LedgerEntry.create(
                to,
                amount,
                LedgerEntry.Direction.CREDIT,
                description,
                now
        );

        ledgerEntryRepository.save(debit);
        ledgerEntryRepository.save(credit);
    }

    @Override
    public Money getBalance(AccountId accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        List<LedgerEntry> entries = ledgerEntryRepository.findByAccount(accountId);
        Money balance = Money.zero(account.currency());

        for (LedgerEntry entry : entries) {
            if (entry.direction() == LedgerEntry.Direction.DEBIT) {
                balance = balance.subtract(entry.amount());
            } else {
                balance = balance.add(entry.amount());
            }
        }

        return balance;
    }

    @Override
    public List<Account> listAccounts() {
        return accountRepository.findAll();
    }
}
