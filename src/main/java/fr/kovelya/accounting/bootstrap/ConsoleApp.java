package fr.kovelya.accounting.bootstrap;

import fr.kovelya.accounting.application.*;
import fr.kovelya.accounting.domain.account.Account;
import fr.kovelya.accounting.domain.account.AccountType;
import fr.kovelya.accounting.domain.customer.Customer;
import fr.kovelya.accounting.domain.invoice.SalesInvoice;
import fr.kovelya.accounting.domain.ledger.JournalTransaction;
import fr.kovelya.accounting.domain.ledger.JournalType;
import fr.kovelya.accounting.domain.period.AccountingPeriod;
import fr.kovelya.accounting.domain.shared.Money;
import fr.kovelya.accounting.domain.tax.TaxCategory;
import fr.kovelya.accounting.domain.tax.VatRate;
import fr.kovelya.accounting.infrastructure.persistence.memory.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

public class ConsoleApp {

    public static void main(String[] args) {
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        InMemoryLedgerEntryRepository ledgerEntryRepository = new InMemoryLedgerEntryRepository();
        InMemoryJournalTransactionRepository transactionRepository = new InMemoryJournalTransactionRepository();
        InMemoryAccountingPeriodRepository periodRepository = new InMemoryAccountingPeriodRepository();
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
        InMemorySalesInvoiceRepository salesInvoiceRepository = new InMemorySalesInvoiceRepository();

        AccountingService accountingService = new AccountingServiceImpl(
                accountRepository,
                ledgerEntryRepository,
                transactionRepository,
                periodRepository
        );

        InvoicingService invoicingService = new InvoicingServiceImpl(
                customerRepository,
                salesInvoiceRepository,
                Currency.getInstance("EUR")
        );

        VatRate vatRate20 = VatRate.ofFraction(new BigDecimal("0.20"));

        InvoicePostingService invoicePostingService = new InvoicePostingServiceImpl(
                salesInvoiceRepository,
                accountRepository,
                accountingService,
                "4110",
                "7060",
                "4457",
                vatRate20
        );

        InvoicePaymentService invoicePaymentService = new InvoicePaymentServiceImpl(
                salesInvoiceRepository,
                accountRepository,
                accountingService,
                "4110"
        );

        AccountingPeriod fy2025 = accountingService.createPeriod(
                "FY-2025",
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31)
        );

        Account cash = accountingService.openAccount("5300", "Cash", "EUR", AccountType.ASSET);
        Account bank = accountingService.openAccount("5121", "Bank", "EUR", AccountType.ASSET);
        Account receivable = accountingService.openAccount("4110", "Accounts Receivable", "EUR", AccountType.ASSET);
        Account revenue = accountingService.openAccount("7060", "Sales Revenue", "EUR", AccountType.INCOME);
        Account vatCollected = accountingService.openAccount("4457", "VAT Collected", "EUR", AccountType.LIABILITY);

        Money amount = Money.of(new BigDecimal("100.00"), Currency.getInstance("EUR"));
        accountingService.transfer(bank.id(), cash.id(), amount, JournalType.GENERAL, "Initial transfer");

        Customer customer = invoicingService.createCustomer("CUST-001", "Acme Corp");

        SalesInvoice invoice = invoicingService.createDraftInvoice(
                "INV-2025-0001",
                customer.id(),
                LocalDate.of(2025, 1, 15),
                LocalDate.of(2025, 2, 15),
                new InvoiceLineRequest("Website development", new BigDecimal("1500.00"), TaxCategory.STANDARD),
                new InvoiceLineRequest("Maintenance plan", new BigDecimal("200.00"), TaxCategory.STANDARD),
                new InvoiceLineRequest("Maintenance plan", new BigDecimal("200.00"), TaxCategory.EXEMPT)
        );

        invoicePostingService.postInvoice(invoice.id());
        invoicePaymentService.recordPayment(invoice.id(), "5121");

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
                            + " - " + line.accountType() + " - balance: " + line.balance()
            );
        }

        System.out.println("Customers:");
        for (Customer c : invoicingService.listCustomers()) {
            System.out.println(c.code() + " - " + c.name());
        }

        System.out.println("Sales invoices:");
        for (SalesInvoice inv : invoicingService.listInvoices()) {
            System.out.println(inv.number() + " - " + inv.status() + " - total: " + inv.total());
        }
    }
}
