import fr.kovelya.accounting.domain.ledger.JournalType;
import fr.kovelya.accounting.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CustomerUnallocatedPaymentTest {

    @Test
    void unallocatedCustomerPaymentPostsTo4191AndIsIdempotent() {
        var ctx = TestBootstrap.bootstrap();

        var customer = ctx.invoicingService().createCustomer(ctx.ledgerId(), "CUST-ADV-001", "Acme");
        var eur = Currency.getInstance("EUR");
        UUID cmd = UUID.randomUUID();

        ctx.customerAdvanceReceiptService().recordUnallocatedPayment(
                cmd,
                customer.id(),
                "5121",
                Money.of(new BigDecimal("100.00"), eur),
                LocalDate.of(2025, 1, 5)
        );

        ctx.customerAdvanceReceiptService().recordUnallocatedPayment(
                cmd,
                customer.id(),
                "5121",
                Money.of(new BigDecimal("100.00"), eur),
                LocalDate.of(2025, 1, 5)
        );

        assertEquals(1, ctx.transactionRepository().findByJournal(JournalType.BANK).size());
        assertTrue(ctx.transactionRepository().findByJournalAndReference(JournalType.BANK, "BANK-ADV-CUST-" + cmd).isPresent());

        var bank = ctx.accountRepository().findByCode("5121").orElseThrow();
        var adv = ctx.accountRepository().findByCode("4191").orElseThrow();

        assertEquals(1, ctx.ledgerEntryRepository().findByAccount(bank.id()).size());
        assertEquals(1, ctx.ledgerEntryRepository().findByAccount(adv.id()).size());
    }
}
