package fr.kovelya.accounting.domain.customer;

import fr.kovelya.accounting.domain.ledger.LedgerId;

import java.util.Objects;

public class Customer {

    private final CustomerId id;
    private final LedgerId ledgerId;
    private final String code;
    private final String name;
    private final boolean active;

    public Customer(CustomerId id, LedgerId ledgerId, String code, String name, boolean active) {
        if (ledgerId == null) {
            throw new IllegalArgumentException("Ledger is required");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Customer code is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Customer name is required");
        }
        this.id = Objects.requireNonNull(id);
        this.ledgerId = ledgerId;
        this.code = code;
        this.name = name;
        this.active = active;
    }

    public static Customer create(LedgerId ledgerId, String code, String name) {
        return new Customer(CustomerId.newId(), ledgerId, code, name, true);
    }

    public CustomerId id() {
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

    public boolean isActive() {
        return active;
    }

    public Customer deactivate() {
        return new Customer(id, ledgerId, code, name, false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer customer)) return false;
        return Objects.equals(id, customer.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
