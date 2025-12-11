package fr.kovelya.accounting.domain.supplier;

import java.util.Objects;

public final class Supplier {

    private final SupplierId id;
    private final String code;
    private final String name;

    public Supplier(SupplierId id, String code, String name) {
        this.id = Objects.requireNonNull(id);
        this.code = Objects.requireNonNull(code);
        this.name = Objects.requireNonNull(name);
    }

    public static Supplier create(String code, String name) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Supplier code is required");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Supplier name is required");
        }

        return new Supplier(SupplierId.newId(), code, name);
    }

    public SupplierId id() {
        return id;
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }
}
