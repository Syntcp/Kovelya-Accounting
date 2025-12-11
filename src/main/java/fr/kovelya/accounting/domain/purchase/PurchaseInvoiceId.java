package fr.kovelya.accounting.domain.purchase;

import java.util.UUID;

public record PurchaseInvoiceId(UUID value) {

    public static PurchaseInvoiceId newId() {
        return new PurchaseInvoiceId(UUID.randomUUID());
    }
}
