import fr.kovelya.accounting.application.dto.InvoiceLineRequest;
import fr.kovelya.accounting.domain.shared.Money;
import fr.kovelya.accounting.domain.tax.TaxCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CustomerOverpaymentTest {

    @Test
    void overpaymentCreatesCustomerCreditAndPostsTo4191() {
        var ctx = TestBootstrap.bootstrap();

        var customer = ctx.invoicingService().createCustomer(ctx.ledgerId(), "CUST-001", "Acme");
        var invoice = ctx.invoicingService().createDraftInvoice(
                ctx.ledgerId(),
                "INV-OVER-0001",
                customer.id(),
                LocalDate.of(2025, 1, 10),
                LocalDate.of(2025, 2, 10),
                new InvoiceLineRequest("Service", new BigDecimal("120.00"), TaxCategory.STANDARD)
        );

        ctx.invoicePostingService().postInvoice(invoice.id());

        ctx.invoicePaymentService().recordPayment(
                UUID.randomUUID(),
                invoice.id(),
                "5121",
                Money.of(new BigDecimal("130.00"), Currency.getInstance("EUR")),
                LocalDate.of(2025, 1, 15)
        );

        var ar = ctx.accountRepository().findByCode("4110").orElseThrow();
        var advances = ctx.accountRepository().findByCode("4191").orElseThrow();

        assertEquals(0, ctx.accountingService().getBalance(ar.id()).amount().compareTo(BigDecimal.ZERO));
        assertEquals(0, ctx.accountingService().getBalance(advances.id()).amount().compareTo(new BigDecimal("10.00")));

        var credits = ctx.customerCreditRepository().findOpenByCustomer(customer.id());
        assertEquals(1, credits.size());
        assertEquals(0, credits.get(0).remaining().amount().compareTo(new BigDecimal("10.00")));
    }
}
