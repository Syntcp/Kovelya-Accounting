package fr.kovelya.bootstrap;

import fr.kovelya.application.AccountingService;
import fr.kovelya.application.AccountingServiceImpl;
import fr.kovelya.domain.model.Account;
import fr.kovelya.domain.model.Money;
import fr.kovelya.infrastructure.persistence.memory.InMemoryAccountRepository;
import fr.kovelya.infrastructure.persistence.memory.InMemoryLedgerEntryRepository;

import java.math.BigDecimal;
import java.util.Currency;

public class ConsoleApp {
    public static void main(String[] args) {
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        InMemoryLedgerEntryRepository ledgerEntryRepository = new InMemoryLedgerEntryRepository();
        AccountingService accountingService = new AccountingServiceImpl(accountRepository, ledgerEntryRepository);

        Account cash = accountingService.openAccount("Cash", "EUR");
        Account bank = accountingService.openAccount("Bank", "EUR");

        Money amount = Money.of(new BigDecimal("100.00"), Currency.getInstance("EUR"));
        accountingService.transfer(cash.id(), bank.id(), amount, "Initial transfer");

        System.out.println("Kovelya Extreme Accounting is alive");

        for (Account account : accountingService.listAccounts()) {
            Money balance = accountingService.getBalance(account.id());
            System.out.println(account.id().value() + " - " + account.name() + " - balance: " + balance);
        }
    }
}