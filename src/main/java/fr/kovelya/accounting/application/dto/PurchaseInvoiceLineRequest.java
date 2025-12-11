package fr.kovelya.accounting.application.dto;

import fr.kovelya.accounting.domain.tax.TaxCategory;

import java.math.BigDecimal;
import java.util.Objects;

public final class PurchaseInvoiceLineRequest {

    private final String description;
    private final BigDecimal amount;
    private final TaxCategory taxCategory;

    public PurchaseInvoiceLineRequest(String description, BigDecimal amount, TaxCategory taxCategory) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Line description is required");
        }
        this.description = description;
        this.amount = Objects.requireNonNull(amount);
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
