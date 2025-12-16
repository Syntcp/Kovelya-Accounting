import fr.kovelya.accounting.application.dto.InvoiceLineRequest;
import fr.kovelya.accounting.domain.shared.Money;
import fr.kovelya.accounting.domain.tax.TaxCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CustomerCreditApplyTest {

    @Test
    void applyCreditPaysAnotherInvoiceWithoutBankMovement() {
        var ctx = TestBootstrap.bootstrap();

        var customer = ctx.invoicingService().createCustomer(ctx.ledgerId(), "CUST-001", "Acme");

        var inv1 = ctx.invoicingService().createDraftInvoice(
                ctx.ledgerId(),
                "INV-CR-0001",
                customer.id(),
                LocalDate.of(2025, 1, 10),
                LocalDate.of(2025, 2, 10),
                new InvoiceLineRequest("Service", new BigDecimal("120.00"), TaxCategory.STANDARD)
        );
        ctx.invoicePostingService().postInvoice(inv1.id());

        ctx.invoicePaymentService().recordPayment(
                UUID.randomUUID(),
                inv1.id(),
                "5121",
                Money.of(new BigDecimal("130.00"), Currency.getInstance("EUR")),
                LocalDate.of(2025, 1, 15)
        );

        var inv2 = ctx.invoicingService().createDraftInvoice(
                ctx.ledgerId(),
                "INV-CR-0002",
                customer.id(),
                LocalDate.of(2025, 1, 20),
                LocalDate.of(2025, 2, 20),
                new InvoiceLineRequest("Service", new BigDecimal("120.00"), TaxCategory.STANDARD)
        );
        ctx.invoicePostingService().postInvoice(inv2.id());

        ctx.customerCreditApplicationService().applyCredit(
                UUID.randomUUID(),
                inv2.id(),
                Money.of(new BigDecimal("10.00"), Currency.getInstance("EUR")),
                LocalDate.of(2025, 1, 25)
        );

        var ar = ctx.accountRepository().findByCode("4110").orElseThrow();
        var adv = ctx.accountRepository().findByCode("4191").orElseThrow();

        assertEquals(0, ctx.accountingService().getBalance(adv.id()).amount().compareTo(BigDecimal.ZERO));
        assertEquals(0, ctx.accountingService().getBalance(ar.id()).amount().compareTo(new BigDecimal("110.00")));
    }
}
