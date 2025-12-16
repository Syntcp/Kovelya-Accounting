import fr.kovelya.accounting.application.dto.InvoiceLineRequest;
import fr.kovelya.accounting.domain.ledger.JournalType;
import fr.kovelya.accounting.domain.shared.Money;
import fr.kovelya.accounting.domain.tax.TaxCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

public class SalesCreditNoteTest {

    @Test
    void creditNoteCancelsInvoiceBalances() {
        var ctx = TestBootstrap.bootstrap();

        var customer = ctx.invoicingService().createCustomer(ctx.ledgerId(), "CUST-001", "Acme");
        var invoice = ctx.invoicingService().createDraftInvoice(
                ctx.ledgerId(),
                "INV-CN-0001",
                customer.id(),
                LocalDate.of(2025, 1, 10),
                LocalDate.of(2025, 2, 10),
                new InvoiceLineRequest("Service", new BigDecimal("120.00"), TaxCategory.STANDARD)
        );

        ctx.invoicePostingService().postInvoice(invoice.id());

        var ar = ctx.accountRepository().findByCode("4110").orElseThrow();
        var rev = ctx.accountRepository().findByCode("7060").orElseThrow();
        var vat = ctx.accountRepository().findByCode("4457").orElseThrow();

        assertEquals(0, ctx.accountingService().getBalance(ar.id()).amount().compareTo(new BigDecimal("120.00")));
        assertEquals(0, ctx.accountingService().getBalance(rev.id()).amount().compareTo(new BigDecimal("100.00")));
        assertEquals(0, ctx.accountingService().getBalance(vat.id()).amount().compareTo(new BigDecimal("20.00")));

        ctx.salesCreditNoteService().issueFullCreditNote(
                invoice.id(),
                "CN-2025-0001",
                LocalDate.of(2025, 1, 20)
        );

        assertEquals(0, ctx.accountingService().getBalance(ar.id()).amount().compareTo(BigDecimal.ZERO));
        assertEquals(0, ctx.accountingService().getBalance(rev.id()).amount().compareTo(BigDecimal.ZERO));
        assertEquals(0, ctx.accountingService().getBalance(vat.id()).amount().compareTo(BigDecimal.ZERO));
    }

    @Test
    void creditNoteIsIdempotentByNumber() {
        var ctx = TestBootstrap.bootstrap();

        var customer = ctx.invoicingService().createCustomer(ctx.ledgerId(), "CUST-001", "Acme");
        var invoice = ctx.invoicingService().createDraftInvoice(
                ctx.ledgerId(),
                "INV-CN-0002",
                customer.id(),
                LocalDate.of(2025, 1, 10),
                LocalDate.of(2025, 2, 10),
                new InvoiceLineRequest("Service", new BigDecimal("120.00"), TaxCategory.STANDARD)
        );

        ctx.invoicePostingService().postInvoice(invoice.id());

        ctx.salesCreditNoteService().issueFullCreditNote(invoice.id(), "CN-2025-0002", LocalDate.of(2025, 1, 20));
        ctx.salesCreditNoteService().issueFullCreditNote(invoice.id(), "CN-2025-0002", LocalDate.of(2025, 1, 20));

        assertEquals(2, ctx.transactionRepository().findByJournal(JournalType.SALES).size());
        assertTrue(ctx.transactionRepository().findByJournalAndReference(JournalType.SALES, "CN-2025-0002").isPresent());
    }

    @Test
    void partialCreditNoteReducesInvoiceBalances() {
        var ctx = TestBootstrap.bootstrap();

        var customer = ctx.invoicingService().createCustomer(ctx.ledgerId(), "CUST-001", "Acme");
        var invoice = ctx.invoicingService().createDraftInvoice(
                ctx.ledgerId(),
                "INV-PCN-0001",
                customer.id(),
                LocalDate.of(2025, 1, 10),
                LocalDate.of(2025, 2, 10),
                new InvoiceLineRequest("Service", new BigDecimal("120.00"), TaxCategory.STANDARD)
        );

        ctx.invoicePostingService().postInvoice(invoice.id());

        var ar = ctx.accountRepository().findByCode("4110").orElseThrow();
        var rev = ctx.accountRepository().findByCode("7060").orElseThrow();
        var vat = ctx.accountRepository().findByCode("4457").orElseThrow();

        assertEquals(0, ctx.accountingService().getBalance(ar.id()).amount().compareTo(new BigDecimal("120.00")));
        assertEquals(0, ctx.accountingService().getBalance(rev.id()).amount().compareTo(new BigDecimal("100.00")));
        assertEquals(0, ctx.accountingService().getBalance(vat.id()).amount().compareTo(new BigDecimal("20.00")));

        ctx.salesCreditNoteService().issuePartialCreditNote(
                invoice.id(),
                "CN-PART-0001",
                LocalDate.of(2025, 1, 20),
                Money.of(new BigDecimal("60.00"), Currency.getInstance("EUR"))
        );

        assertEquals(0, ctx.accountingService().getBalance(ar.id()).amount().compareTo(new BigDecimal("60.00")));
        assertEquals(0, ctx.accountingService().getBalance(rev.id()).amount().compareTo(new BigDecimal("50.00")));
        assertEquals(0, ctx.accountingService().getBalance(vat.id()).amount().compareTo(new BigDecimal("10.00")));
    }

    @Test
    void partialCreditNoteCannotExceedInvoiceTotal() {
        var ctx = TestBootstrap.bootstrap();

        var customer = ctx.invoicingService().createCustomer(ctx.ledgerId(), "CUST-001", "Acme");
        var invoice = ctx.invoicingService().createDraftInvoice(
                ctx.ledgerId(),
                "INV-PCN-0002",
                customer.id(),
                LocalDate.of(2025, 1, 10),
                LocalDate.of(2025, 2, 10),
                new InvoiceLineRequest("Service", new BigDecimal("120.00"), TaxCategory.STANDARD)
        );

        ctx.invoicePostingService().postInvoice(invoice.id());

        assertThrows(IllegalArgumentException.class, () -> ctx.salesCreditNoteService().issuePartialCreditNote(
                invoice.id(),
                "CN-PART-0002",
                LocalDate.of(2025, 1, 20),
                Money.of(new BigDecimal("130.00"), Currency.getInstance("EUR"))
        ));
    }
}
