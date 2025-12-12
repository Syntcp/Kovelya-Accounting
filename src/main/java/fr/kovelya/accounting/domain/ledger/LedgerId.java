package fr.kovelya.accounting.domain.ledger;

import java.util.Objects;
import java.util.UUID;

public final class LedgerId {

    private final UUID value;

    public LedgerId(UUID value) {
        this.value = value;
    }

    public static LedgerId newId() {
        return new LedgerId(UUID.randomUUID());
    }

    public static LedgerId of(UUID value) {
        return new LedgerId(value);
    }

    public UUID value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LedgerId ledgerId)) return false;
        return Objects.equals(value, ledgerId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
