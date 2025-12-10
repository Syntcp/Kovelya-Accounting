package fr.kovelya.accounting.domain.period;

import java.util.Objects;
import java.util.UUID;

public final class AccountingPeriodId {

    private final String value;

    public AccountingPeriodId(String value) {
        this.value = value;
    }

    public static AccountingPeriodId newId() {
        return new AccountingPeriodId(UUID.randomUUID().toString());
    }

    public static AccountingPeriodId of(String value) {
        return new AccountingPeriodId(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccountingPeriodId that)) return false;
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
