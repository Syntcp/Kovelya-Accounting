import fr.kovelya.accounting.application.dto.PurchaseInvoiceLineRequest;
import fr.kovelya.accounting.application.dto.PurchaseInvoicePaymentAllocation;
import fr.kovelya.accounting.domain.ledger.JournalType;
import fr.kovelya.accounting.domain.shared.Money;
import fr.kovelya.accounting.domain.tax.TaxCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class SupplierBatchPaymentTest {

    @Test
    void oneSupplierBankPaymentCanPayMultiplePurchaseInvoices() {
        var ctx = TestBootstrap.bootstrap();

        var sup = ctx.purchasingService().createSupplier(ctx.ledgerId(), "SUP-001", "Supplier");

        var inv1 = ctx.purchasingService().createDraftPurchaseInvoice(
                ctx.ledgerId(),
                "PINV-BATCH-0001",
                sup.id(),
                LocalDate.of(2025, 1, 10),
                LocalDate.of(2025, 2, 10),
                new PurchaseInvoiceLineRequest("Subcontract", new BigDecimal("120.00"), TaxCategory.STANDARD)
        );

        var inv2 = ctx.purchasingService().createDraftPurchaseInvoice(
                ctx.ledgerId(),
                "PINV-BATCH-0002",
                sup.id(),
                LocalDate.of(2025, 1, 12),
                LocalDate.of(2025, 2, 12),
                new PurchaseInvoiceLineRequest("Subcontract", new BigDecimal("120.00"), TaxCategory.STANDARD)
        );

        ctx.purchaseInvoicePostingService().postPurchaseInvoice(inv1.id());
        ctx.purchaseInvoicePostingService().postPurchaseInvoice(inv2.id());

        UUID cmd = UUID.randomUUID();
        ctx.supplierBatchPaymentService().recordBatchPayment(
                cmd,
                "5121",
                LocalDate.of(2025, 1, 15),
                new PurchaseInvoicePaymentAllocation(inv1.id(), Money.of(new BigDecimal("120.00"), Currency.getInstance("EUR"))),
                new PurchaseInvoicePaymentAllocation(inv2.id(), Money.of(new BigDecimal("120.00"), Currency.getInstance("EUR")))
        );

        assertEquals(1, ctx.transactionRepository().findByJournal(JournalType.BANK).size());
        assertEquals(1, ctx.supplierPaymentRepository().findByInvoice(inv1.id()).size());
        assertEquals(1, ctx.supplierPaymentRepository().findByInvoice(inv2.id()).size());
    }
}
