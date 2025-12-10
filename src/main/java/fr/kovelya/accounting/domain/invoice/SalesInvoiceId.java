package fr.kovelya.accounting.domain.invoice;

import java.util.Objects;
import java.util.UUID;

public final class SalesInvoiceId {

    private final String value;

    public SalesInvoiceId(String value) {
        this.value = value;
    }

    public static SalesInvoiceId newId() {
        return new SalesInvoiceId(UUID.randomUUID().toString());
    }

    public static SalesInvoiceId of(String value) {
        return new SalesInvoiceId(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SalesInvoiceId that)) return false;
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
