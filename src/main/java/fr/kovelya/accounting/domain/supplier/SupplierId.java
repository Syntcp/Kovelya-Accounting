package fr.kovelya.accounting.domain.supplier;

import java.util.UUID;

public record SupplierId(UUID value) {

    public static SupplierId newId() {
        return new SupplierId(UUID.randomUUID());
    }
}
