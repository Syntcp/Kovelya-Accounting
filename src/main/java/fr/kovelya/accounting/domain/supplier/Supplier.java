package fr.kovelya.accounting.domain.supplier;

import fr.kovelya.accounting.domain.ledger.LedgerId;

import java.util.Objects;

public final class Supplier {

    private final SupplierId id;
    private final LedgerId ledgerId;
    private final String code;
    private final String name;

    public Supplier(SupplierId id, LedgerId ledgerId, String code, String name) {
        if (ledgerId == null) {
            throw new IllegalArgumentException("Ledger is required");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Supplier code is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Supplier name is required");
        }
        this.id = Objects.requireNonNull(id);
        this.ledgerId = ledgerId;
        this.code = code;
        this.name = name;
    }

    public static Supplier create(LedgerId ledgerId, String code, String name) {
        return new Supplier(SupplierId.newId(), ledgerId, code, name);
    }

    public SupplierId id() {
        return id;
    }

    public LedgerId ledgerId() {
        return ledgerId;
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }
}
