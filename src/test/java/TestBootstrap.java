import fr.kovelya.accounting.application.service.*;
import fr.kovelya.accounting.application.service.impl.*;
import fr.kovelya.accounting.domain.account.Account;
import fr.kovelya.accounting.domain.account.AccountType;
import fr.kovelya.accounting.domain.ledger.LedgerId;
import fr.kovelya.accounting.domain.period.AccountingPeriod;
import fr.kovelya.accounting.domain.repository.SupplierAdvanceRepository;
import fr.kovelya.accounting.domain.tax.VatRate;
import fr.kovelya.accounting.infrastructure.persistence.memory.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

public class TestBootstrap {

    public static Context bootstrap() {
        var accountRepository = new InMemoryAccountRepository();
        var ledgerEntryRepository = new InMemoryLedgerEntryRepository();
        var transactionRepository = new InMemoryJournalTransactionRepository();
        var periodRepository = new InMemoryAccountingPeriodRepository();
        var customerRepository = new InMemoryCustomerRepository();
        var salesInvoiceRepository = new InMemorySalesInvoiceRepository();
        var customerPaymentRepository = new InMemoryCustomerPaymentRepository();
        var idempotencyRepository = new InMemoryIdempotencyRepository();
        var supplierRepository = new InMemorySupplierRepository();
        var purchaseInvoiceRepository = new InMemoryPurchaseInvoiceRepository();
        var supplierPaymentRepository = new InMemorySupplierPaymentRepository();
        var customerCreditRepository = new InMemoryCustomerCreditRepository();
        var idempotencyExecutor = new IdempotencyExecutor(idempotencyRepository);
        var supplierAdvanceRepository = new InMemorySupplierAdvanceRepository();

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

        PeriodClosingService periodClosingService = new PeriodClosingServiceImpl(
                accountingService,
                periodRepository,
                accountRepository
        );

        VatRate vatRate20 = VatRate.ofFraction(new BigDecimal("0.20"));

        PurchasingService purchasingService = new PurchasingServiceImpl(
                supplierRepository,
                purchaseInvoiceRepository,
                Currency.getInstance("EUR")
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
                supplierAdvanceRepository,
                "4010",
                "4090",
                idempotencyExecutor
        );

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
                customerCreditRepository,
                "4110",
                "4191",
                idempotencyExecutor
        );

        SalesCreditNoteService salesCreditNoteService = new SalesCreditNoteServiceImpl(
                salesInvoiceRepository,
                accountRepository,
                accountingService,
                transactionRepository,
                "4110",
                "7060",
                "4457",
                vatRate20
        );

        CustomerCreditApplicationService customerCreditApplicationService = new CustomerCreditApplicationServiceImpl(
                salesInvoiceRepository,
                customerPaymentRepository,
                customerCreditRepository,
                accountRepository,
                accountingService,
                "4110",
                "4191",
                idempotencyExecutor
        );

        SupplierAdvanceApplicationService supplierAdvanceApplicationService = new SupplierAdvanceApplicationServiceImpl(
                purchaseInvoiceRepository,
                supplierPaymentRepository,
                supplierAdvanceRepository,
                accountRepository,
                accountingService,
                "4010",
                "4091",
                idempotencyExecutor
        );


        FinancialStatementsService financialStatementsService = new FinancialStatementsServiceImpl(accountingService, transactionRepository);

        LedgerId ledgerId = new LedgerId(UUID.randomUUID());
        AccountingPeriod period = accountingService.createPeriod(
                ledgerId,
                "TEST",
                LocalDate.of(2000, 1, 1),
                LocalDate.of(2100, 12, 31)
        );

        Account bank = accountingService.openAccount(ledgerId, "5121", "Bank", "EUR", AccountType.ASSET);
        Account capital = accountingService.openAccount(ledgerId, "1010", "Capital", "EUR", AccountType.EQUITY);
        Account retained = accountingService.openAccount(ledgerId, "1100", "Retained earnings", "EUR", AccountType.EQUITY);

        accountingService.openAccount(ledgerId, "4110", "Accounts Receivable", "EUR", AccountType.ASSET);
        accountingService.openAccount(ledgerId, "7060", "Sales Revenue", "EUR", AccountType.INCOME);
        accountingService.openAccount(ledgerId, "4457", "VAT Collected", "EUR", AccountType.LIABILITY);
        accountingService.openAccount(ledgerId, "4010", "Suppliers Payable", "EUR", AccountType.LIABILITY);
        accountingService.openAccount(ledgerId, "6060", "Subcontracting", "EUR", AccountType.EXPENSE);
        accountingService.openAccount(ledgerId, "4456", "VAT Deductible", "EUR", AccountType.ASSET);
        accountingService.openAccount(ledgerId, "4191", "Customer Advances", "EUR", AccountType.LIABILITY);
        accountingService.openAccount(ledgerId, "4091", "Supplier Advances", "EUR", AccountType.ASSET);

        return new Context(
                accountRepository,
                ledgerEntryRepository,
                transactionRepository,
                periodRepository,
                customerRepository,
                salesInvoiceRepository,
                customerPaymentRepository,
                supplierRepository,
                purchaseInvoiceRepository,
                supplierPaymentRepository,
                accountingService,
                invoicingService,
                purchasingService,
                invoicePostingService,
                invoicePaymentService,
                purchaseInvoicePostingService,
                purchaseInvoicePaymentService,
                periodClosingService,
                financialStatementsService,
                salesCreditNoteService,
                customerCreditRepository,
                customerCreditApplicationService,
                supplierAdvanceApplicationService,
                supplierAdvanceRepository,
                ledgerId,
                period,
                bank,
                capital,
                retained
        );
    }

    public record Context(
            InMemoryAccountRepository accountRepository,
            InMemoryLedgerEntryRepository ledgerEntryRepository,
            InMemoryJournalTransactionRepository transactionRepository,
            InMemoryAccountingPeriodRepository periodRepository,
            InMemoryCustomerRepository customerRepository,
            InMemorySalesInvoiceRepository salesInvoiceRepository,
            InMemoryCustomerPaymentRepository customerPaymentRepository,
            InMemorySupplierRepository supplierRepository,
            InMemoryPurchaseInvoiceRepository purchaseInvoiceRepository,
            InMemorySupplierPaymentRepository supplierPaymentRepository,
            AccountingService accountingService,
            InvoicingService invoicingService,
            PurchasingService purchasingService,
            InvoicePostingService invoicePostingService,
            InvoicePaymentService invoicePaymentService,
            PurchaseInvoicePostingService purchaseInvoicePostingService,
            PurchaseInvoicePaymentService purchaseInvoicePaymentService,
            PeriodClosingService periodClosingService,
            FinancialStatementsService financialStatementsService,
            SalesCreditNoteService salesCreditNoteService,
            InMemoryCustomerCreditRepository customerCreditRepository,
            CustomerCreditApplicationService customerCreditApplicationService,
            SupplierAdvanceApplicationService supplierAdvanceApplicationService,
            SupplierAdvanceRepository supplierAdvanceRepository,
            LedgerId ledgerId,
            AccountingPeriod period,
            Account bank,
            Account capital,
            Account retainedEarnings
    ) {}
}
