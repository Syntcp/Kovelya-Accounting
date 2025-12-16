import fr.kovelya.accounting.application.dto.PurchaseInvoiceLineRequest;
import fr.kovelya.accounting.domain.shared.Money;
import fr.kovelya.accounting.domain.tax.TaxCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class SupplierOverpaymentTest {

    @Test
    void overpaymentCreatesSupplierAdvanceAndPostsTo4091() {
        var ctx = TestBootstrap.bootstrap();

        var supplier = ctx.purchasingService().createSupplier(ctx.ledgerId(), "SUP-001", "Supplier");
        var inv = ctx.purchasingService().createDraftPurchaseInvoice(
                ctx.ledgerId(),
                "PINV-ADV-0001",
                supplier.id(),
                LocalDate.of(2025, 1, 10),
                LocalDate.of(2025, 2, 10),
                new PurchaseInvoiceLineRequest("Subcontract", new BigDecimal("120.00"), TaxCategory.STANDARD)
        );

        ctx.purchaseInvoicePostingService().postPurchaseInvoice(inv.id());

        ctx.purchaseInvoicePaymentService().recordPayment(
                UUID.randomUUID(),
                inv.id(),
                "5121",
                Money.of(new BigDecimal("130.00"), Currency.getInstance("EUR")),
                LocalDate.of(2025, 1, 15)
        );

        var payable = ctx.accountRepository().findByCode("4010").orElseThrow();
        var adv = ctx.accountRepository().findByCode("4091").orElseThrow();

        assertEquals(0, ctx.accountingService().getBalance(payable.id()).amount().compareTo(BigDecimal.ZERO));
        assertEquals(0, ctx.accountingService().getBalance(adv.id()).amount().compareTo(new BigDecimal("10.00")));

        var advances = ctx.supplierAdvanceRepository().findOpenBySupplier(supplier.id());
        assertEquals(1, advances.size());
        assertEquals(0, advances.get(0).remaining().amount().compareTo(new BigDecimal("10.00")));
    }
}
