import fr.kovelya.accounting.application.dto.AccountPosting;
import fr.kovelya.accounting.application.dto.InvoiceLineRequest;
import fr.kovelya.accounting.domain.ledger.JournalType;
import fr.kovelya.accounting.domain.ledger.LedgerEntry;
import fr.kovelya.accounting.domain.shared.Money;
import fr.kovelya.accounting.domain.tax.TaxCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AccountingEngineTest {

    @Test
    void unbalancedTransactionDoesNotPersistAnything() {
        var ctx = TestBootstrap.bootstrap();
        var eur = Currency.getInstance("EUR");

        var debit = new AccountPosting(
                ctx.bank().id(),
                Money.of(new BigDecimal("100"), eur),
                LedgerEntry.Direction.DEBIT
        );

        var credit = new AccountPosting(
                ctx.capital().id(),
                Money.of(new BigDecimal("90"), eur),
                LedgerEntry.Direction.CREDIT
        );

        assertThrows(IllegalArgumentException.class, () -> ctx.accountingService().postJournalTransaction(
                JournalType.GENERAL,
                "TEST-UNBALANCED",
                "Unbalanced",
                LocalDate.of(2025, 1, 1),
                debit,
                credit
        ));

        assertEquals(0, ctx.ledgerEntryRepository().findByAccount(ctx.bank().id()).size());
        assertEquals(0, ctx.transactionRepository().findAll().size());
    }

    @Test
    void invoicePostingIsIdempotent() {
        var ctx = TestBootstrap.bootstrap();

        var customer = ctx.invoicingService().createCustomer(ctx.ledgerId(), "CUST-001", "Acme");

        var invoice = ctx.invoicingService().createDraftInvoice(
                ctx.ledgerId(),
                "INV-TEST-0001",
                customer.id(),
                LocalDate.of(2025, 1, 10),
                LocalDate.of(2025, 2, 10),
                new InvoiceLineRequest("Service", new BigDecimal("120.00"), TaxCategory.STANDARD)
        );

        ctx.invoicePostingService().postInvoice(invoice.id());
        ctx.invoicePostingService().postInvoice(invoice.id());

        assertTrue(ctx.transactionRepository().findByJournalAndReference(JournalType.SALES, "INV-TEST-0001").isPresent());
        assertEquals(1, ctx.transactionRepository().findByJournal(JournalType.SALES).size());

        var ar = ctx.accountRepository().findByCode("4110").orElseThrow();
        var rev = ctx.accountRepository().findByCode("7060").orElseThrow();
        var vat = ctx.accountRepository().findByCode("4457").orElseThrow();

        assertEquals(1, ctx.ledgerEntryRepository().findByAccount(ar.id()).size());
        assertEquals(1, ctx.ledgerEntryRepository().findByAccount(rev.id()).size());
        assertEquals(1, ctx.ledgerEntryRepository().findByAccount(vat.id()).size());
    }

    @Test
    void paymentIdempotencyPreventsDoublePayment() {
        var ctx = TestBootstrap.bootstrap();
        var customer = ctx.invoicingService().createCustomer(ctx.ledgerId(), "CUST-001", "Acme");

        var invoice = ctx.invoicingService().createDraftInvoice(
                ctx.ledgerId(),
                "INV-TEST-0002",
                customer.id(),
                LocalDate.of(2025, 1, 10),
                LocalDate.of(2025, 2, 10),
                new InvoiceLineRequest("Service", new BigDecimal("120.00"), TaxCategory.STANDARD)
        );

        ctx.invoicePostingService().postInvoice(invoice.id());

        UUID cmd = UUID.randomUUID();
        ctx.invoicePaymentService().recordPayment(
                cmd,
                invoice.id(),
                "5121",
                Money.of(new BigDecimal("10.00"), Currency.getInstance("EUR")),
                LocalDate.of(2025, 1, 15)
        );

        ctx.invoicePaymentService().recordPayment(
                cmd,
                invoice.id(),
                "5121",
                Money.of(new BigDecimal("10.00"), Currency.getInstance("EUR")),
                LocalDate.of(2025, 1, 15)
        );

        assertEquals(1, ctx.customerPaymentRepository().findByInvoice(invoice.id()).size());
        assertEquals(1, ctx.transactionRepository().findByJournal(JournalType.BANK).size());
    }

    @Test
    void cannotPostInClosedPeriod() {
        var ctx = TestBootstrap.bootstrap();

        ctx.periodClosingService().closePeriod(ctx.period(), ctx.retainedEarnings().id());

        var eur = Currency.getInstance("EUR");
        var debit = new AccountPosting(ctx.bank().id(), Money.of(new BigDecimal("1"), eur), LedgerEntry.Direction.DEBIT);
        var credit = new AccountPosting(ctx.capital().id(), Money.of(new BigDecimal("1"), eur), LedgerEntry.Direction.CREDIT);

        assertThrows(IllegalStateException.class, () -> ctx.accountingService().postJournalTransaction(
                JournalType.GENERAL,
                "TEST-AFTER-CLOSE",
                "Should fail",
                LocalDate.of(2025, 1, 1),
                debit,
                credit
        ));
    }

    @Test
    void balanceSheetBalancesAfterClosing() {
        var ctx = TestBootstrap.bootstrap();

        var customer = ctx.invoicingService().createCustomer(ctx.ledgerId(), "CUST-001", "Acme");
        var invoice = ctx.invoicingService().createDraftInvoice(
                ctx.ledgerId(),
                "INV-TEST-0003",
                customer.id(),
                LocalDate.of(2025, 1, 10),
                LocalDate.of(2025, 2, 10),
                new InvoiceLineRequest("Service", new BigDecimal("120.00"), TaxCategory.STANDARD)
        );

        ctx.invoicePostingService().postInvoice(invoice.id());
        ctx.periodClosingService().closePeriod(ctx.period(), ctx.retainedEarnings().id());

        var bs = ctx.financialStatementsService().getBalanceSheet(ctx.period());
        assertEquals(bs.totalEquity(), bs.derivedEquity());
    }

    @Test
    void operatingIncomeStatementIgnoresClosingEntries() {
        var ctx = TestBootstrap.bootstrap();

        var customer = ctx.invoicingService().createCustomer(ctx.ledgerId(), "CUST-001", "Acme");
        var invoice = ctx.invoicingService().createDraftInvoice(
                ctx.ledgerId(),
                "INV-TEST-OPER-0001",
                customer.id(),
                LocalDate.of(2025, 1, 10),
                LocalDate.of(2025, 2, 10),
                new InvoiceLineRequest("Service", new BigDecimal("120.00"), TaxCategory.STANDARD)
        );

        ctx.invoicePostingService().postInvoice(invoice.id());

        ctx.periodClosingService().closePeriod(ctx.period(), ctx.retainedEarnings().id());

        var closed = ctx.financialStatementsService().getIncomeStatement(ctx.period());
        assertEquals(0, closed.totalRevenue().amount().compareTo(BigDecimal.ZERO));
        assertEquals(0, closed.netIncome().amount().compareTo(BigDecimal.ZERO));

        var operating = ctx.financialStatementsService().getOperatingIncomeStatement(ctx.period());
        assertEquals(0, operating.totalRevenue().amount().compareTo(new BigDecimal("100")));
        assertEquals(0, operating.netIncome().amount().compareTo(new BigDecimal("100")));
    }

    @Test
    void reversingTransactionRestoresBalances() {
        var ctx = TestBootstrap.bootstrap();
        var eur = Currency.getInstance("EUR");

        var debit = new AccountPosting(
                ctx.bank().id(),
                Money.of(new BigDecimal("10.00"), eur),
                LedgerEntry.Direction.DEBIT
        );

        var credit = new AccountPosting(
                ctx.capital().id(),
                Money.of(new BigDecimal("10.00"), eur),
                LedgerEntry.Direction.CREDIT
        );

        ctx.accountingService().postJournalTransaction(
                JournalType.GENERAL,
                "TRF-REV-0001",
                "Funding",
                LocalDate.of(2025, 1, 1),
                debit,
                credit
        );

        var original = ctx.transactionRepository()
                .findByJournalAndReference(JournalType.GENERAL, "TRF-REV-0001")
                .orElseThrow();

        ctx.accountingService().reverseTransaction(
                original.id(),
                "REV-TRF-REV-0001",
                "Reversal",
                LocalDate.of(2025, 1, 2)
        );

        var bankBal = ctx.accountingService().getBalance(ctx.bank().id());
        var capBal = ctx.accountingService().getBalance(ctx.capital().id());

        assertEquals(0, bankBal.amount().compareTo(BigDecimal.ZERO));
        assertEquals(0, capBal.amount().compareTo(BigDecimal.ZERO));
    }

    @Test
    void reversingTransactionIsIdempotentByReference() {
        var ctx = TestBootstrap.bootstrap();
        var eur = Currency.getInstance("EUR");

        var debit = new AccountPosting(
                ctx.bank().id(),
                Money.of(new BigDecimal("10.00"), eur),
                LedgerEntry.Direction.DEBIT
        );

        var credit = new AccountPosting(
                ctx.capital().id(),
                Money.of(new BigDecimal("10.00"), eur),
                LedgerEntry.Direction.CREDIT
        );

        ctx.accountingService().postJournalTransaction(
                JournalType.GENERAL,
                "TRF-REV-0002",
                "Funding",
                LocalDate.of(2025, 1, 1),
                debit,
                credit
        );

        var original = ctx.transactionRepository()
                .findByJournalAndReference(JournalType.GENERAL, "TRF-REV-0002")
                .orElseThrow();

        ctx.accountingService().reverseTransaction(
                original.id(),
                "REV-IDEMP-0001",
                "Reversal",
                LocalDate.of(2025, 1, 2)
        );

        ctx.accountingService().reverseTransaction(
                original.id(),
                "REV-IDEMP-0001",
                "Reversal",
                LocalDate.of(2025, 1, 2)
        );

        assertEquals(1, ctx.transactionRepository().findByJournal(JournalType.ADJUSTMENT).size());
        assertEquals(2, ctx.ledgerEntryRepository().findByAccount(ctx.bank().id()).size());
    }
}
