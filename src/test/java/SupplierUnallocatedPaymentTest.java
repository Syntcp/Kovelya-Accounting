import fr.kovelya.accounting.domain.ledger.JournalType;
import fr.kovelya.accounting.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class SupplierUnallocatedPaymentTest {

    @Test
    void unallocatedSupplierPaymentPostsTo4091AndIsIdempotent() {
        var ctx = TestBootstrap.bootstrap();

        var supplier = ctx.purchasingService().createSupplier(ctx.ledgerId(), "SUP-ADV-001", "Supplier");
        var eur = Currency.getInstance("EUR");
        UUID cmd = UUID.randomUUID();

        ctx.supplierAdvanceReceiptService().recordUnallocatedPayment(
                cmd,
                supplier.id(),
                "5121",
                Money.of(new BigDecimal("50.00"), eur),
                LocalDate.of(2025, 1, 6)
        );

        ctx.supplierAdvanceReceiptService().recordUnallocatedPayment(
                cmd,
                supplier.id(),
                "5121",
                Money.of(new BigDecimal("50.00"), eur),
                LocalDate.of(2025, 1, 6)
        );

        assertEquals(1, ctx.transactionRepository().findByJournal(JournalType.BANK).size());
        assertTrue(ctx.transactionRepository().findByJournalAndReference(JournalType.BANK, "BANK-ADV-SUP-" + cmd).isPresent());

        var bank = ctx.accountRepository().findByCode("5121").orElseThrow();
        var adv = ctx.accountRepository().findByCode("4091").orElseThrow();

        assertEquals(1, ctx.ledgerEntryRepository().findByAccount(bank.id()).size());
        assertEquals(1, ctx.ledgerEntryRepository().findByAccount(adv.id()).size());
    }
}
