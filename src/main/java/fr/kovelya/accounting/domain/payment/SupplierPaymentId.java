package fr.kovelya.accounting.domain.payment;

import java.util.UUID;

public record SupplierPaymentId(UUID value) {

    public static SupplierPaymentId newId() {
        return new SupplierPaymentId(UUID.randomUUID());
    }
}
