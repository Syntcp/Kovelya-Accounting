package fr.kovelya.accounting.domain.advance;

import fr.kovelya.accounting.domain.shared.Money;
import fr.kovelya.accounting.domain.supplier.SupplierId;

import java.util.UUID;

public record SupplierAdvance(SupplierAdvanceId id, SupplierId supplierId, Money remaining, UUID sourceCommandId) {

    public static SupplierAdvance create(SupplierId supplierId, Money remaining, UUID sourceCommandId) {
        return new SupplierAdvance(new SupplierAdvanceId(UUID.randomUUID()), supplierId, remaining, sourceCommandId);
    }

    public SupplierAdvance consume(Money amount) {
        return new SupplierAdvance(id, supplierId, remaining.subtract(amount), sourceCommandId);
    }
}
