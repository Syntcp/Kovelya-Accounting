package fr.kovelya.accounting.domain.customer;

import java.util.Objects;

public class Customer {

    private final CustomerId id;
    private final String code;
    private final String name;
    private final boolean active;

    public Customer(CustomerId id, String code, String name, boolean active) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.active = active;
    }

    public static Customer create(String code, String name) {
        return new Customer(CustomerId.newId(), code, name, true);
    }

    public CustomerId id() {
        return id;
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
        return new Customer(id, code, name, false);
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
