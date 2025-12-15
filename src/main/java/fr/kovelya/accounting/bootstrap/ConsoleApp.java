package fr.kovelya.accounting.bootstrap;

import fr.kovelya.accounting.application.dto.AccountPosting;
import fr.kovelya.accounting.application.dto.InvoiceLineRequest;
import fr.kovelya.accounting.application.dto.PurchaseInvoiceLineRequest;
import fr.kovelya.accounting.application.report.AccountBalanceView;
import fr.kovelya.accounting.application.report.BalanceSheetView;
import fr.kovelya.accounting.application.report.CustomerReceivableAgingView;
import fr.kovelya.accounting.application.report.IncomeStatementView;
import fr.kovelya.accounting.application.report.SupplierPayableAgingView;
import fr.kovelya.accounting.application.service.*;
import fr.kovelya.accounting.application.service.impl.*;
import fr.kovelya.accounting.domain.account.Account;
import fr.kovelya.accounting.domain.account.AccountType;
import fr.kovelya.accounting.domain.customer.Customer;
import fr.kovelya.accounting.domain.invoice.SalesInvoice;
import fr.kovelya.accounting.domain.ledger.JournalTransaction;
import fr.kovelya.accounting.domain.ledger.JournalType;
import fr.kovelya.accounting.domain.ledger.LedgerEntry;
import fr.kovelya.accounting.domain.ledger.LedgerId;
import fr.kovelya.accounting.domain.period.AccountingPeriod;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoice;
import fr.kovelya.accounting.domain.shared.Money;
import fr.kovelya.accounting.domain.supplier.Supplier;
import fr.kovelya.accounting.domain.tax.TaxCategory;
import fr.kovelya.accounting.domain.tax.VatRate;
import fr.kovelya.accounting.infrastructure.persistence.memory.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

public class ConsoleApp {

    public static void main(String[] args) {
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        InMemoryLedgerEntryRepository ledgerEntryRepository = new InMemoryLedgerEntryRepository();
        InMemoryJournalTransactionRepository transactionRepository = new InMemoryJournalTransactionRepository();
        InMemoryAccountingPeriodRepository periodRepository = new InMemoryAccountingPeriodRepository();
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
        InMemorySalesInvoiceRepository salesInvoiceRepository = new InMemorySalesInvoiceRepository();
        InMemorySupplierRepository supplierRepository = new InMemorySupplierRepository();
        InMemoryPurchaseInvoiceRepository purchaseInvoiceRepository = new InMemoryPurchaseInvoiceRepository();
        InMemoryCustomerPaymentRepository customerPaymentRepository = new InMemoryCustomerPaymentRepository();
        InMemorySupplierPaymentRepository supplierPaymentRepository = new InMemorySupplierPaymentRepository();

        InMemoryIdempotencyRepository idempotencyRepository = new InMemoryIdempotencyRepository();
        IdempotencyExecutor idempotencyExecutor = new IdempotencyExecutor(idempotencyRepository);

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

        PurchasingService purchasingService = new PurchasingServiceImpl(
                supplierRepository,
                purchaseInvoiceRepository,
                Currency.getInstance("EUR")
        );

        PeriodClosingService periodClosingService = new PeriodClosingServiceImpl(
                accountingService,
                periodRepository,
                accountRepository
        );

        VatRate vatRate20 = VatRate.ofFraction(new BigDecimal("0.20"));

        InvoicePostingService invoicePostingService = new InvoicePostingServiceImpl(
                salesInvoiceRepository,
                accountRepository,
                accountingService,
                transactionRepository,
                "4110",
                "7060",
                "4457",
                vatRate20
        );

        InvoicePaymentService invoicePaymentService = new InvoicePaymentServiceImpl(
                salesInvoiceRepository,
                accountRepository,
                accountingService,
                customerPaymentRepository,
                "4110",
                idempotencyExecutor
        );

        PurchaseInvoicePostingService purchaseInvoicePostingService = new PurchaseInvoicePostingServiceImpl(
                purchaseInvoiceRepository,
                accountRepository,
                accountingService,
                transactionRepository,
                "4010",
                "6060",
                "4456",
                vatRate20
        );

        PurchaseInvoicePaymentService purchaseInvoicePaymentService = new PurchaseInvoicePaymentServiceImpl(
                purchaseInvoiceRepository,
                accountRepository,
                accountingService,
                supplierPaymentRepository,
                "4010",
                idempotencyExecutor
        );

        FinancialStatementsService financialStatementsService = new FinancialStatementsServiceImpl(accountingService);

        ReceivablesAgingService receivablesAgingService = new ReceivablesAgingServiceImpl(
                customerRepository,
                salesInvoiceRepository,
                customerPaymentRepository
        );

        PayablesAgingService payablesAgingService = new PayablesAgingServiceImpl(
                supplierRepository,
                purchaseInvoiceRepository,
                supplierPaymentRepository
        );

        LedgerId ledgerId = new LedgerId(UUID.randomUUID());

        AccountingPeriod fy2025 = accountingService.createPeriod(
                ledgerId,
                "FY-2025",
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31)
        );

        Account bank = accountingService.openAccount(ledgerId, "5121", "Bank", "EUR", AccountType.ASSET);
        Account cash = accountingService.openAccount(ledgerId, "5300", "Cash", "EUR", AccountType.ASSET);

        Account capital = accountingService.openAccount(ledgerId, "1010", "Capital", "EUR", AccountType.EQUITY);
        Account retainedEarnings = accountingService.openAccount(ledgerId, "1100", "Retained earnings", "EUR", AccountType.EQUITY);

        accountingService.openAccount(ledgerId, "4110", "Accounts Receivable", "EUR", AccountType.ASSET);
        accountingService.openAccount(ledgerId, "7060", "Sales Revenue", "EUR", AccountType.INCOME);
        accountingService.openAccount(ledgerId, "4457", "VAT Collected", "EUR", AccountType.LIABILITY);

        accountingService.openAccount(ledgerId, "4010", "Suppliers Payable", "EUR", AccountType.LIABILITY);
        accountingService.openAccount(ledgerId, "6060", "Subcontracting", "EUR", AccountType.EXPENSE);
        accountingService.openAccount(ledgerId, "4456", "VAT Deductible", "EUR", AccountType.ASSET);

        Money initialFunding = Money.of(new BigDecimal("100.00"), Currency.getInstance("EUR"));
        accountingService.postJournalTransaction(
                JournalType.GENERAL,
                "OPEN-FY-2025-0001",
                "Opening funding",
                LocalDate.of(2025, 1, 1),
                new AccountPosting(bank.id(), initialFunding, LedgerEntry.Direction.DEBIT),
                new AccountPosting(capital.id(), initialFunding, LedgerEntry.Direction.CREDIT)
        );

        Customer customer = invoicingService.createCustomer(ledgerId, "CUST-001", "Acme Corp");

        SalesInvoice invoice1 = invoicingService.createDraftInvoice(
                ledgerId,
                "INV-2025-0001",
                customer.id(),
                LocalDate.of(2025, 1, 15),
                LocalDate.of(2025, 2, 15),
                new InvoiceLineRequest("Consulting services", new BigDecimal("1000.00"), TaxCategory.STANDARD)
        );

        SalesInvoice invoice2 = invoicingService.createDraftInvoice(
                ledgerId,
                "INV-2025-0002",
                customer.id(),
                LocalDate.of(2025, 2, 10),
                LocalDate.of(2025, 3, 10),
                new InvoiceLineRequest("Maintenance services", new BigDecimal("500.00"), TaxCategory.STANDARD)
        );

        invoicePostingService.postInvoice(invoice1.id());
        invoicePostingService.postInvoice(invoice2.id());

        UUID cmd = UUID.randomUUID();

        invoicePaymentService.recordPayment(
                cmd,
                invoice2.id(),
                "5121",
                Money.of(new BigDecimal("300.00"), Currency.getInstance("EUR")),
                LocalDate.of(2025, 3, 1)
        );

        invoicePaymentService.recordPayment(
                cmd,
                invoice2.id(),
                "5121",
                Money.of(new BigDecimal("300.00"), Currency.getInstance("EUR")),
                LocalDate.of(2025, 3, 1)
        );

        Supplier supplier = purchasingService.createSupplier(ledgerId, "SUP-001", "Web Services Ltd");

        PurchaseInvoice purchaseInvoice = purchasingService.createDraftPurchaseInvoice(
                ledgerId,
                "PINV-2025-0001",
                supplier.id(),
                LocalDate.of(2025, 2, 10),
                LocalDate.of(2025, 2, 28),
                new PurchaseInvoiceLineRequest("Subcontracting work", new BigDecimal("1200.00"), TaxCategory.STANDARD)
        );

        purchaseInvoicePostingService.postPurchaseInvoice(purchaseInvoice.id());
        purchaseInvoicePaymentService.recordPayment(
                purchaseInvoice.id(),
                "5121",
                LocalDate.of(2025, 3, 5)
        );

        periodClosingService.closePeriod(fy2025, retainedEarnings.id());

        System.out.println("Kovelya Extreme Accounting is alive");

        System.out.println("Accounts (global balance):");
        for (Account account : accountingService.listAccounts(ledgerId)) {
            Money balance = accountingService.getBalance(account.id());
            System.out.println(account.code() + " - " + account.name() + " - " + account.type() + " - balance: " + balance);
        }

        System.out.println("Accounts (balance in FY-2025):");
        for (Account account : accountingService.listAccounts(ledgerId)) {
            Money periodBalance = accountingService.getBalanceForPeriod(account.id(), fy2025);
            System.out.println(account.code() + " - " + account.name() + " - " + account.type() + " - period balance: " + periodBalance);
        }

        System.out.println("Transactions:");
        for (JournalTransaction transaction : accountingService.listTransactions(ledgerId)) {
            System.out.println(
                    transaction.id().value()
                            + " - " + transaction.journalType()
                            + " - " + transaction.reference()
                            + " - " + transaction.description()
            );
        }

        System.out.println("Trial balance for " + fy2025.name() + ":");
        for (AccountBalanceView line : accountingService.getTrialBalance(ledgerId, fy2025)) {
            System.out.println(
                    line.accountCode()
                            + " - " + line.accountName()
                            + " - " + line.accountType() + " - balance: " + line.balance()
            );
        }

        IncomeStatementView incomeStatement = financialStatementsService.getIncomeStatement(fy2025);
        System.out.println("Income statement for " + fy2025.name() + ":");
        System.out.println("Total revenue: " + incomeStatement.totalRevenue());
        System.out.println("Total expenses: " + incomeStatement.totalExpenses());
        System.out.println("Net income: " + incomeStatement.netIncome());

        BalanceSheetView balanceSheet = financialStatementsService.getBalanceSheet(fy2025);
        System.out.println("Balance sheet for " + fy2025.name() + ":");
        System.out.println("Total assets: " + balanceSheet.totalAssets());
        System.out.println("Total liabilities: " + balanceSheet.totalLiabilities());
        System.out.println("Total equity (accounts): " + balanceSheet.totalEquity());
        System.out.println("Derived equity (A - L): " + balanceSheet.derivedEquity());

        LocalDate asOfDate = LocalDate.of(2025, 3, 15);

        System.out.println("Receivables aging as of " + asOfDate + ":");
        for (CustomerReceivableAgingView view : receivablesAgingService.getCustomerAging(asOfDate)) {
            System.out.println(
                    view.customer().code()
                            + " - " + view.customer().name()
                            + " | not due: " + view.notDue()
                            + " | 0-30: " + view.due0_30()
                            + " | 31-60: " + view.due31_60()
                            + " | 61-90: " + view.due61_90()
                            + " | 90+: " + view.due90Plus()
                            + " | total: " + view.total()
            );
        }

        System.out.println("Payables aging as of " + asOfDate + ":");
        for (SupplierPayableAgingView view : payablesAgingService.getSupplierAging(asOfDate)) {
            System.out.println(
                    view.supplier().code()
                            + " - " + view.supplier().name()
                            + " | not due: " + view.notDue()
                            + " | 0-30: " + view.due0_30()
                            + " | 31-60: " + view.due31_60()
                            + " | 61-90: " + view.due61_90()
                            + " | 90+: " + view.due90Plus()
                            + " | total: " + view.total()
            );
        }
    }
}
