package fr.kovelya.accounting.application.service.impl;

import fr.kovelya.accounting.application.report.AccountBalanceView;
import fr.kovelya.accounting.application.dto.AccountPosting;
import fr.kovelya.accounting.application.service.AccountingService;
import fr.kovelya.accounting.domain.account.Account;
import fr.kovelya.accounting.domain.account.AccountId;
import fr.kovelya.accounting.domain.account.AccountType;
import fr.kovelya.accounting.domain.period.AccountingPeriod;
import fr.kovelya.accounting.domain.ledger.JournalTransaction;
import fr.kovelya.accounting.domain.ledger.JournalType;
import fr.kovelya.accounting.domain.ledger.LedgerEntry;
import fr.kovelya.accounting.domain.shared.Money;
import fr.kovelya.accounting.domain.repository.AccountRepository;
import fr.kovelya.accounting.domain.repository.AccountingPeriodRepository;
import fr.kovelya.accounting.domain.repository.JournalTransactionRepository;
import fr.kovelya.accounting.domain.repository.LedgerEntryRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Comparator;
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

        AccountPosting debit = new AccountPosting(from, amount, LedgerEntry.Direction.DEBIT);
        AccountPosting credit = new AccountPosting(to, amount, LedgerEntry.Direction.CREDIT);

        postJournalTransaction(journalType, description, debit, credit);
    }

    @Override
    public void postJournalTransaction(JournalType journalType, String description, AccountPosting... postings) {
        if (postings == null || postings.length < 2) {
            throw new IllegalArgumentException("At least two postings are required");
        }

        Instant now = Instant.now();
        List<LedgerEntry> entries = new ArrayList<>();
        Currency currency = null;

        for (AccountPosting posting : postings) {
            Account account = accountRepository.findById(posting.accountId())
                    .orElseThrow(() -> new IllegalArgumentException("Account not found"));

            if (currency == null) {
                currency = account.currency();
            } else if (!account.currency().equals(currency)) {
                throw new IllegalArgumentException("Akk accounts in a transaction must share the same currency");
            }

            if(!posting.amount().currency().equals(currency)) {
                throw new IllegalArgumentException("Posting amount currency must match account currency");
            }

            LedgerEntry entry = LedgerEntry.create(
                    posting.accountId(),
                    posting.amount(),
                    posting.direction(),
                    description,
                    now
            );

            ledgerEntryRepository.save(entry);
            entries.add(entry);
        }

        String reference = "TX-" + now.toEpochMilli();
        JournalTransaction transaction = JournalTransaction.create(
                journalType,
                reference,
                description,
                now,
                entries
        );
        journalTransactionRepository.save(transaction);
    }

    @Override
    public Money getBalance(AccountId accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        List<LedgerEntry> entries = ledgerEntryRepository.findByAccount(accountId);

        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;

        for (LedgerEntry entry : entries) {
            BigDecimal amount = entry.amount().amount();
            if (entry.direction() == LedgerEntry.Direction.DEBIT) {
                debitTotal = debitTotal.add(amount);
            } else {
                creditTotal = creditTotal.add(amount);
            }
        }

        BigDecimal net;
        AccountType type = account.type();
        if (type == AccountType.ASSET || type == AccountType.EXPENSE) {
            net = debitTotal.subtract(creditTotal);
        } else {
            net = creditTotal.subtract(debitTotal);
        }
        return Money.of(net, account.currency());
    }

    @Override
    public Money getBalanceForPeriod(AccountId accountId, AccountingPeriod period) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        ZoneId zone = ZoneId.systemDefault();
        Instant from = period.startDate().atStartOfDay(zone).toInstant();
        Instant to = period.endDate().plusDays(1).atStartOfDay(zone).minusNanos(1).toInstant();

        List<LedgerEntry> entries = ledgerEntryRepository.findByAccountAndPeriod(accountId, from, to);

        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;

        for (LedgerEntry entry : entries) {
            BigDecimal amount = entry.amount().amount();
            if (entry.direction() == LedgerEntry.Direction.DEBIT) {
                debitTotal = debitTotal.add(amount);
            } else {
                creditTotal = creditTotal.add(amount);
            }
        }

        BigDecimal net;
        AccountType type = account.type();
        if (type == AccountType.ASSET || type == AccountType.EXPENSE) {
            net = debitTotal.subtract(creditTotal);
        } else {
            net = creditTotal.subtract(debitTotal);
        }
        return Money.of(net, account.currency());
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

    @Override
    public List<AccountBalanceView> getTrialBalance(AccountingPeriod period) {
        List<AccountBalanceView> result = new ArrayList<>();

        for (Account account : accountRepository.findAll()) {
            var balance = getBalanceForPeriod(account.id(), period);
            result.add(new AccountBalanceView(
                    account.code(),
                    account.name(),
                    account.type(),
                    balance
            ));
        }

        result.sort(Comparator.comparing(AccountBalanceView::accountCode));
        return result;
    }
}
