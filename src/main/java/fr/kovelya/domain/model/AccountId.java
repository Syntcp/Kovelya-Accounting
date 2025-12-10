package fr.kovelya.domain.model;

import java.util.Objects;
import java.util.UUID;

public final class AccountId {

    private final String value;

    private AccountId(String value) {
        this.value = value;
    }

    public static AccountId newId() {
        return new AccountId(UUID.randomUUID().toString());
    }

    public static AccountId of(String value) {
        return new AccountId(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccountId that)) return false;
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
