import fr.kovelya.accounting.application.dto.PurchaseInvoiceLineRequest;
import fr.kovelya.accounting.domain.ledger.JournalType;
import fr.kovelya.accounting.domain.shared.Money;
import fr.kovelya.accounting.domain.tax.TaxCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class PurchaseFlowTest {

    @Test
    void purchaseInvoicePostingIsIdempotent() {
        var ctx = TestBootstrap.bootstrap();

        var supplier = ctx.purchasingService().createSupplier(ctx.ledgerId(), "SUP-001", "Supplier");
        var invoice = ctx.purchasingService().createDraftPurchaseInvoice(
                ctx.ledgerId(),
                "PINV-TEST-0001",
                supplier.id(),
                LocalDate.of(2025, 1, 10),
                LocalDate.of(2025, 2, 10),
                new PurchaseInvoiceLineRequest("Subcontract", new BigDecimal("120.00"), TaxCategory.STANDARD)
        );

        ctx.purchaseInvoicePostingService().postPurchaseInvoice(invoice.id());
        ctx.purchaseInvoicePostingService().postPurchaseInvoice(invoice.id());

        assertTrue(ctx.transactionRepository().findByJournalAndReference(JournalType.PURCHASES, "PINV-TEST-0001").isPresent());
        assertEquals(1, ctx.transactionRepository().findByJournal(JournalType.PURCHASES).size());
    }

    @Test
    void purchasePaymentIdempotencyPreventsDoublePayment() {
        var ctx = TestBootstrap.bootstrap();

        var supplier = ctx.purchasingService().createSupplier(ctx.ledgerId(), "SUP-001", "Supplier");
        var invoice = ctx.purchasingService().createDraftPurchaseInvoice(
                ctx.ledgerId(),
                "PINV-TEST-0002",
                supplier.id(),
                LocalDate.of(2025, 1, 10),
                LocalDate.of(2025, 2, 10),
                new PurchaseInvoiceLineRequest("Subcontract", new BigDecimal("120.00"), TaxCategory.STANDARD)
        );

        ctx.purchaseInvoicePostingService().postPurchaseInvoice(invoice.id());

        UUID cmd = UUID.randomUUID();
        ctx.purchaseInvoicePaymentService().recordPayment(
                cmd,
                invoice.id(),
                "5121",
                Money.of(new BigDecimal("10.00"), Currency.getInstance("EUR")),
                LocalDate.of(2025, 1, 15)
        );
        ctx.purchaseInvoicePaymentService().recordPayment(
                cmd,
                invoice.id(),
                "5121",
                Money.of(new BigDecimal("10.00"), Currency.getInstance("EUR")),
                LocalDate.of(2025, 1, 15)
        );

        assertEquals(1, ctx.supplierPaymentRepository().findByInvoice(invoice.id()).size());
        assertEquals(1, ctx.transactionRepository().findByJournal(JournalType.BANK).size());
    }
}
