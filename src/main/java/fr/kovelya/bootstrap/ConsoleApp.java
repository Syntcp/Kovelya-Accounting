package fr.kovelya.bootstrap;

import fr.kovelya.application.AccountBalanceView;
import fr.kovelya.application.AccountingService;
import fr.kovelya.application.AccountingServiceImpl;
import fr.kovelya.domain.account.Account;
import fr.kovelya.domain.account.AccountType;
import fr.kovelya.domain.period.AccountingPeriod;
import fr.kovelya.domain.ledger.JournalTransaction;
import fr.kovelya.domain.ledger.JournalType;
import fr.kovelya.domain.shared.Money;
import fr.kovelya.infrastructure.persistence.memory.InMemoryAccountRepository;
import fr.kovelya.infrastructure.persistence.memory.InMemoryAccountingPeriodRepository;
import fr.kovelya.infrastructure.persistence.memory.InMemoryJournalTransactionRepository;
import fr.kovelya.infrastructure.persistence.memory.InMemoryLedgerEntryRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

public class ConsoleApp {

    public static void main(String[] args) {
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        InMemoryLedgerEntryRepository ledgerEntryRepository = new InMemoryLedgerEntryRepository();
        InMemoryJournalTransactionRepository transactionRepository = new InMemoryJournalTransactionRepository();
        InMemoryAccountingPeriodRepository periodRepository = new InMemoryAccountingPeriodRepository();

        AccountingService accountingService = new AccountingServiceImpl(
                accountRepository,
                ledgerEntryRepository,
                transactionRepository,
                periodRepository
        );

        AccountingPeriod fy2025 = accountingService.createPeriod(
                "FY-2025",
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31)
        );

        Account cash = accountingService.openAccount("5300", "Cash", "EUR", AccountType.ASSET);
        Account bank = accountingService.openAccount("5121", "Bank", "EUR", AccountType.ASSET);

        Money amount = Money.of(new BigDecimal("100.00"), Currency.getInstance("EUR"));
        accountingService.transfer(cash.id(), bank.id(), amount, JournalType.GENERAL, "Initial transfer");

        System.out.println("Kovelya Extreme Accounting is alive");

        System.out.println("Accounts (global balance):");
        for (Account account : accountingService.listAccounts()) {
            Money balance = accountingService.getBalance(account.id());
            System.out.println(account.code() + " - " + account.name() + " - " + account.type() + " - balance: " + balance);
        }

        System.out.println("Accounts (balance in FY-2025):");
        for (Account account : accountingService.listAccounts()) {
            Money periodBalance = accountingService.getBalanceForPeriod(account.id(), fy2025);
            System.out.println(account.code() + " - " + account.name() + " - " + account.type() + " - period balance: " + periodBalance);
        }

        System.out.println("Transactions:");
        for (JournalTransaction transaction : accountingService.listTransactions()) {
            System.out.println(
                    transaction.id().value()
                            + " - " + transaction.journalType()
                            + " - " + transaction.reference()
                            + " - " + transaction.description()
            );
        }

        System.out.println("Trial balance for " + fy2025.name() + ":");
        for (AccountBalanceView line : accountingService.getTrialBalance(fy2025)) {
            System.out.println(
                    line.accountCode()
                            + " - " + line.accountName()
                            + " - " + line.accountType()
                            + " - balance: " + line.balance()
            );
        }
    }
}
