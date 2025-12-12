package fr.kovelya.accounting.domain.payment;

import java.util.UUID;

public record CustomerPaymentId(UUID value) {

    public static CustomerPaymentId newId() {
        return new CustomerPaymentId(UUID.randomUUID());
    }
}
