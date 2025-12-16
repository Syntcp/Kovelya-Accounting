import fr.kovelya.accounting.application.dto.PurchaseInvoiceLineRequest;
import fr.kovelya.accounting.domain.shared.Money;
import fr.kovelya.accounting.domain.tax.TaxCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class SupplierAdvanceApplyTest {

    @Test
    void applyAdvanceReducesPayableWithoutBankMovement() {
        var ctx = TestBootstrap.bootstrap();

        var supplier = ctx.purchasingService().createSupplier(ctx.ledgerId(), "SUP-001", "Supplier");

        var inv1 = ctx.purchasingService().createDraftPurchaseInvoice(
                ctx.ledgerId(),
                "PINV-ADV-0002",
                supplier.id(),
                LocalDate.of(2025, 1, 10),
                LocalDate.of(2025, 2, 10),
                new PurchaseInvoiceLineRequest("Subcontract", new BigDecimal("120.00"), TaxCategory.STANDARD)
        );
        ctx.purchaseInvoicePostingService().postPurchaseInvoice(inv1.id());

        ctx.purchaseInvoicePaymentService().recordPayment(
                UUID.randomUUID(),
                inv1.id(),
                "5121",
                Money.of(new BigDecimal("130.00"), Currency.getInstance("EUR")),
                LocalDate.of(2025, 1, 15)
        );

        var inv2 = ctx.purchasingService().createDraftPurchaseInvoice(
                ctx.ledgerId(),
                "PINV-ADV-0003",
                supplier.id(),
                LocalDate.of(2025, 1, 20),
                LocalDate.of(2025, 2, 20),
                new PurchaseInvoiceLineRequest("Subcontract", new BigDecimal("120.00"), TaxCategory.STANDARD)
        );
        ctx.purchaseInvoicePostingService().postPurchaseInvoice(inv2.id());

        ctx.supplierAdvanceApplicationService().applyAdvance(
                UUID.randomUUID(),
                inv2.id(),
                Money.of(new BigDecimal("10.00"), Currency.getInstance("EUR")),
                LocalDate.of(2025, 1, 25)
        );

        var payable = ctx.accountRepository().findByCode("4010").orElseThrow();
        var adv = ctx.accountRepository().findByCode("4091").orElseThrow();

        assertEquals(0, ctx.accountingService().getBalance(adv.id()).amount().compareTo(BigDecimal.ZERO));
        assertEquals(0, ctx.accountingService().getBalance(payable.id()).amount().compareTo(new BigDecimal("110.00")));
    }
}
