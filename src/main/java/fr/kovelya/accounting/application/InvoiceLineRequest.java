package fr.kovelya.accounting.application;

import fr.kovelya.accounting.domain.tax.TaxCategory;

import java.math.BigDecimal;
import java.util.Objects;

public final class InvoiceLineRequest {

    private final String description;
    private final BigDecimal amount;
    private final TaxCategory taxCategory;

    public InvoiceLineRequest(String description, BigDecimal amount, TaxCategory taxCategory) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Line description is required");
        }

        this.description = description;
        this.amount = amount;
        this.taxCategory = Objects.requireNonNull(taxCategory);
    }

    public String description() {
        return description;
    }

    public BigDecimal amount() {
        return amount;
    }

    public TaxCategory taxCategory() {
        return taxCategory;
    }
}
