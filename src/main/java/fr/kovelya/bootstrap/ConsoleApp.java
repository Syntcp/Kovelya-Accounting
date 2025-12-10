package fr.kovelya.bootstrap;

import fr.kovelya.application.AccountingService;
import fr.kovelya.application.AccountingServiceImpl;
import fr.kovelya.domain.model.Account;
import fr.kovelya.domain.model.AccountType;
import fr.kovelya.domain.model.JournalTransaction;
import fr.kovelya.domain.model.JournalType;
import fr.kovelya.domain.model.Money;
import fr.kovelya.infrastructure.persistence.memory.InMemoryAccountRepository;
import fr.kovelya.infrastructure.persistence.memory.InMemoryJournalTransactionRepository;
import fr.kovelya.infrastructure.persistence.memory.InMemoryLedgerEntryRepository;

import java.math.BigDecimal;
import java.util.Currency;

public class ConsoleApp {

    public static void main(String[] args) {
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        InMemoryLedgerEntryRepository ledgerEntryRepository = new InMemoryLedgerEntryRepository();
        InMemoryJournalTransactionRepository transactionRepository = new InMemoryJournalTransactionRepository();

        AccountingService accountingService = new AccountingServiceImpl(
                accountRepository,
                ledgerEntryRepository,
                transactionRepository
        );

        Account cash = accountingService.openAccount("5300", "Cash", "EUR", AccountType.ASSET);
        Account bank = accountingService.openAccount("5121", "Bank", "EUR", AccountType.ASSET);

        Money amount = Money.of(new BigDecimal("100.00"), Currency.getInstance("EUR"));
        accountingService.transfer(cash.id(), bank.id(), amount, JournalType.GENERAL, "Initial transfer");

        System.out.println("Kovelya Extreme Accounting is alive");

        System.out.println("Accounts:");
        for (Account account : accountingService.listAccounts()) {
            Money balance = accountingService.getBalance(account.id());
            System.out.println(account.code() + " - " + account.name() + " - " + account.type() + " - balance: " + balance);
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
    }
}
