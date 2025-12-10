package fr.kovelya.application;

import fr.kovelya.domain.model.Account;
import fr.kovelya.domain.model.AccountId;
import fr.kovelya.domain.model.AccountType;
import fr.kovelya.domain.model.AccountingPeriod;
import fr.kovelya.domain.model.JournalTransaction;
import fr.kovelya.domain.model.JournalType;
import fr.kovelya.domain.model.LedgerEntry;
import fr.kovelya.domain.model.Money;
import fr.kovelya.domain.repository.AccountRepository;
import fr.kovelya.domain.repository.AccountingPeriodRepository;
import fr.kovelya.domain.repository.JournalTransactionRepository;
import fr.kovelya.domain.repository.LedgerEntryRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

public final class AccountingServiceImpl implements AccountingService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final JournalTransactionRepository journalTransactionRepository;
    private final AccountingPeriodRepository accountingPeriodRepository;

    public AccountingServiceImpl(AccountRepository accountRepository, LedgerEntryRepository ledgerEntryRepository, JournalTransactionRepository journalTransactionRepository, AccountingPeriodRepository accountingPeriodRepository) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.journalTransactionRepository = journalTransactionRepository;
        this.accountingPeriodRepository = accountingPeriodRepository;
    }

    @Override
    public Account openAccount(String code, String name, String currencyCode, AccountType type) {
        Currency currency = Currency.getInstance(currencyCode);
        Account account = Account.open(code, name, type, currency);
        return accountRepository.save(account);
    }

    @Override
    public void transfer(AccountId from, AccountId to, Money amount, JournalType journalType, String description) {
        if (amount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        Account fromAccount = accountRepository.findById(from)
                .orElseThrow(() -> new IllegalArgumentException("Source account not found"));
        Account toAccount = accountRepository.findById(to)
                .orElseThrow(() -> new IllegalArgumentException("Destination account not found"));

        if (!fromAccount.currency().equals(toAccount.currency())) {
            throw new IllegalArgumentException("Currency mismatch between accounts");
        }

        if (!fromAccount.currency().equals(amount.currency())) {
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

        List<LedgerEntry> entries = new ArrayList<>();
        entries.add(debit);
        entries.add(credit);

        String reference = "TX-" + now.toEpochMilli();
        JournalTransaction transaction = JournalTransaction.create(journalType, reference, description, now, entries);
        journalTransactionRepository.save(transaction);
    }

    @Override
    public Money getBalance(AccountId accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        List<LedgerEntry> entries = ledgerEntryRepository.findByAccount(accountId);
        return getMoney(account, entries);
    }

    @Override
    public Money getBalanceForPeriod(AccountId accountId, AccountingPeriod period) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        ZoneId zone = ZoneId.systemDefault();
        Instant from = period.startDate().atStartOfDay(zone).toInstant();
        Instant to = period.endDate().plusDays(1).atStartOfDay(zone).minusNanos(1).toInstant();

        List<LedgerEntry> entries = ledgerEntryRepository.findByAccountAndPeriod(accountId, from, to);
        return getMoney(account, entries);
    }

    private Money getMoney(Account account, List<LedgerEntry> entries) {
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

    @Override
    public List<JournalTransaction> listTransactions() {
        return journalTransactionRepository.findAll();
    }

    @Override
    public AccountingPeriod createPeriod(String name, LocalDate startDate, LocalDate endDate) {
        AccountingPeriod period = AccountingPeriod.open(name, startDate, endDate);
        return accountingPeriodRepository.save(period);
    }

    @Override
    public List<AccountingPeriod> listPeriods() {
        return accountingPeriodRepository.findAll();
    }
}
