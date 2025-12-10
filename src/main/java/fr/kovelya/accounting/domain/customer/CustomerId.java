package fr.kovelya.accounting.domain.customer;

import java.util.Objects;
import java.util.UUID;

public final class CustomerId {

    private final String value;

    public CustomerId(String value) {
        this.value = value;
    }

    public static CustomerId newId() {
        return new CustomerId(UUID.randomUUID().toString());
    }
    public static CustomerId of(String value) {
        return new CustomerId(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if(!(o instanceof CustomerId that)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
