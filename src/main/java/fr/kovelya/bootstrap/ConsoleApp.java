package fr.kovelya.bootstrap;

import fr.kovelya.application.AccountingService;
import fr.kovelya.application.AccountingServiceImpl;
import fr.kovelya.domain.model.Account;
import fr.kovelya.domain.model.Money;
import fr.kovelya.infrastructure.persistence.memory.InMemoryAccountRepository;

import java.math.BigDecimal;
import java.util.Currency;

public class ConsoleApp {
    public static void main(String[] args) {
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        AccountingService accountingService = new AccountingServiceImpl(accountRepository);

        Account cash = accountingService.openAccount("Cash", "EUR");
        Account bank = accountingService.openAccount("Bank", "EUR");
        System.out.println("Kovelya Extreme Accounting is alive");
        for(Account account : accountingService.listAccounts()) {
            System.out.println(account.id().value() + " - " + account.name() + " - " + account.currency().getCurrencyCode());
        }
    }
}
