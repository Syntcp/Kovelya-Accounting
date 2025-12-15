package fr.kovelya.accounting.application.service.impl;

import fr.kovelya.accounting.application.report.AccountBalanceView;
import fr.kovelya.accounting.application.dto.AccountPosting;
import fr.kovelya.accounting.application.service.AccountingService;
import fr.kovelya.accounting.domain.account.Account;
import fr.kovelya.accounting.domain.account.AccountId;
import fr.kovelya.accounting.domain.account.AccountType;
import fr.kovelya.accounting.domain.ledger.LedgerId;
import fr.kovelya.accounting.domain.period.AccountingPeriod;
import fr.kovelya.accounting.domain.ledger.JournalTransaction;
import fr.kovelya.accounting.domain.ledger.JournalType;
import fr.kovelya.accounting.domain.ledger.LedgerEntry;
import fr.kovelya.accounting.domain.period.AccountingPeriodId;
import fr.kovelya.accounting.domain.period.PeriodStatus;
import fr.kovelya.accounting.domain.shared.Money;
import fr.kovelya.accounting.domain.repository.AccountRepository;
import fr.kovelya.accounting.domain.repository.AccountingPeriodRepository;
import fr.kovelya.accounting.domain.repository.JournalTransactionRepository;
import fr.kovelya.accounting.domain.repository.LedgerEntryRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

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
    public Account openAccount(LedgerId ledgerId, String code, String name, String currencyCode, AccountType type) {
        Currency currency = Currency.getInstance(currencyCode);
        Account account = Account.open(ledgerId, code, name, type, currency);
        return accountRepository.save(account);
    }

    @Override
    public void postTransfer(AccountId debitAccountId, AccountId creditAccountId, Money amount, JournalType journalType, String description, LocalDate transactionDate) {
        if (amount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        Account debitAccount = accountRepository.findById(debitAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Debit account not found"));
        Account creditAccount = accountRepository.findById(creditAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Credit account not found"));

        if (!debitAccount.currency().equals(creditAccount.currency())) {
            throw new IllegalArgumentException("Currency mismatch between accounts");
        }

        if (!debitAccount.currency().equals(amount.currency())) {
            throw new IllegalArgumentException("Currency mismatch between account and amount");
        }

        AccountPosting debit = new AccountPosting(debitAccountId, amount, LedgerEntry.Direction.DEBIT);
        AccountPosting credit = new AccountPosting(creditAccountId, amount, LedgerEntry.Direction.CREDIT);

        String reference = buildTransferReference(journalType, debitAccount, creditAccount, transactionDate);

        postJournalTransaction(journalType, reference, description, transactionDate, debit, credit);
    }


    @Override
    public void postJournalTransaction(JournalType journalType, String reference, String description, LocalDate transactionDate, AccountPosting... postings) {
        if (postings == null || postings.length < 2) {
            throw new IllegalArgumentException("At least two postings are required");
        }

        if (transactionDate == null) {
            throw new IllegalArgumentException("Transaction date is required");
        }

        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("Reference is required");
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
                throw new IllegalArgumentException("All accounts in a transaction must share the same currency");
            }

            if (!posting.amount().currency().equals(currency)) {
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

       AccountingPeriod period = requireOpenPeriod(transactionDate);

        JournalTransaction transaction = JournalTransaction.create(
                journalType,
                reference,
                description,
                now,
                transactionDate,
                period.id(),
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
    public List<Account> listAccounts(LedgerId ledgerId) {
        List<Account> result = new ArrayList<>();
        for(Account account : accountRepository.findAll()) {
            if (account.ledgerId().equals(ledgerId)) {
                result.add(account);
            }
        }
        return result;
    }

    @Override
    public List<JournalTransaction> listTransactions(LedgerId ledgerId) {
        Set<AccountingPeriodId> periodIds = new HashSet<>();
        for (AccountingPeriod period : accountingPeriodRepository.findAll()) {
            if (period.ledgerId().equals(ledgerId)) {
                periodIds.add(period.id());
            }
        }

        List<JournalTransaction> result = new ArrayList<>();
        for (JournalTransaction tx : journalTransactionRepository.findAll()) {
            if (periodIds.contains(tx.getPeriodId())) {
                result.add(tx);
            }
        }

        result.sort(
                Comparator.comparing(JournalTransaction::getTransactionDate)
                    .thenComparing(JournalTransaction::timestamp)
        );

        return result;
    }

    @Override
    public List<AccountingPeriod> listPeriods(LedgerId ledgerId) {
        List<AccountingPeriod> result = new ArrayList<>();
        for (AccountingPeriod period : accountingPeriodRepository.findAll()) {
            if (period.ledgerId().equals(ledgerId)) {
                result.add(period);
            }
        }
        return result;
    }

    @Override
    public AccountingPeriod createPeriod(LedgerId ledgerId, String name, LocalDate startDate, LocalDate endDate) {
        AccountingPeriod period = AccountingPeriod.open(ledgerId, name, startDate, endDate);
        return accountingPeriodRepository.save(period);
    }

    @Override
    public List<AccountBalanceView> getTrialBalance(LedgerId ledgerId, AccountingPeriod period) {
        List<AccountBalanceView> result = new ArrayList<>();
        for (Account account : accountRepository.findAll()) {
            if (!account.ledgerId().equals(ledgerId)) {
                continue;
            }
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

    private String buildTransferReference(JournalType journalType, Account debitAccount, Account creditAccount, LocalDate transactionDate) {
        String datePart = transactionDate.toString().replace("-", "");
        long timePart = Instant.now().toEpochMilli();
        return journalType.name() + "-TRF-" + datePart + "-" + debitAccount.code() + "-" + creditAccount.code() + "-" + timePart;
    }

    private AccountingPeriod requireOpenPeriod(LocalDate transactionDate) {
        AccountingPeriod period = accountingPeriodRepository.findByDate(transactionDate)
                .orElseThrow(() -> new IllegalStateException("No accounting period covering date " + transactionDate));

        if (period.status() != PeriodStatus.OPEN) {
            throw new IllegalStateException("Accounting period " + period.name() + " is not open");
        }
        return period;
    }

}
