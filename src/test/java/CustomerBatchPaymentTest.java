import fr.kovelya.accounting.application.dto.InvoiceLineRequest;
import fr.kovelya.accounting.application.dto.SalesInvoicePaymentAllocation;
import fr.kovelya.accounting.domain.invoice.InvoiceStatus;
import fr.kovelya.accounting.domain.ledger.JournalType;
import fr.kovelya.accounting.domain.shared.Money;
import fr.kovelya.accounting.domain.tax.TaxCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CustomerBatchPaymentTest {

    @Test
    void oneBankPaymentCanPayMultipleInvoices() {
        var ctx = TestBootstrap.bootstrap();

        var cust = ctx.invoicingService().createCustomer(ctx.ledgerId(), "CUST-001", "Acme");

        var inv1 = ctx.invoicingService().createDraftInvoice(
                ctx.ledgerId(),
                "INV-BATCH-0001",
                cust.id(),
                LocalDate.of(2025, 1, 10),
                LocalDate.of(2025, 2, 10),
                new InvoiceLineRequest("Service", new BigDecimal("120.00"), TaxCategory.STANDARD)
        );

        var inv2 = ctx.invoicingService().createDraftInvoice(
                ctx.ledgerId(),
                "INV-BATCH-0002",
                cust.id(),
                LocalDate.of(2025, 1, 12),
                LocalDate.of(2025, 2, 12),
                new InvoiceLineRequest("Service", new BigDecimal("120.00"), TaxCategory.STANDARD)
        );

        ctx.invoicePostingService().postInvoice(inv1.id());
        ctx.invoicePostingService().postInvoice(inv2.id());

        UUID cmd = UUID.randomUUID();
        ctx.customerBatchPaymentService().recordBatchPayment(
                cmd,
                "5121",
                LocalDate.of(2025, 1, 15),
                new SalesInvoicePaymentAllocation(inv1.id(), Money.of(new BigDecimal("120.00"), Currency.getInstance("EUR"))),
                new SalesInvoicePaymentAllocation(inv2.id(), Money.of(new BigDecimal("120.00"), Currency.getInstance("EUR")))
        );

        assertEquals(1, ctx.transactionRepository().findByJournal(JournalType.BANK).size());
        assertEquals(1, ctx.customerPaymentRepository().findByInvoice(inv1.id()).size());
        assertEquals(1, ctx.customerPaymentRepository().findByInvoice(inv2.id()).size());

        var r1 = ctx.salesInvoiceRepository().findById(inv1.id()).orElseThrow();
        var r2 = ctx.salesInvoiceRepository().findById(inv2.id()).orElseThrow();

        assertEquals(InvoiceStatus.PAID, r1.status());
        assertEquals(InvoiceStatus.PAID, r2.status());
    }
}
