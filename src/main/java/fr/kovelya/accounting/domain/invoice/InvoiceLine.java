package fr.kovelya.accounting.domain.invoice;

import fr.kovelya.accounting.domain.shared.Money;

import java.util.Objects;

public final class InvoiceLine {

    private final String description;
    private final Money amount;

    public InvoiceLine(String description, Money amount) {
        if(description == null || description.isBlank()) {
            throw new IllegalArgumentException("Line description is required");
        }
        this.description = description;
        this.amount = Objects.requireNonNull(amount);
    }

    public String description() {
        return description;
    }

    public Money amount() {
        return amount;
    }
}
