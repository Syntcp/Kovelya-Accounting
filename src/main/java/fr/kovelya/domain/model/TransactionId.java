package fr.kovelya.domain.model;

import java.util.Objects;
import java.util.UUID;

public final class TransactionId {

    private final String value;

    private TransactionId(String value) {
        this.value = value;
    }

    public static TransactionId newId() {
        return new TransactionId(UUID.randomUUID().toString());
    }

    public static TransactionId of(String value) {
        return new TransactionId(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionId that)) return false;
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
