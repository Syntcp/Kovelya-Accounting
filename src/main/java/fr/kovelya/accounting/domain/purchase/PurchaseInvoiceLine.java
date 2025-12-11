package fr.kovelya.accounting.domain.purchase;

import fr.kovelya.accounting.domain.shared.Money;
import fr.kovelya.accounting.domain.tax.TaxCategory;

import java.util.Objects;

public final class PurchaseInvoiceLine {

    private final String description;
    private final Money amount;
    private final TaxCategory taxCategory;

    public PurchaseInvoiceLine(String description, Money amount, TaxCategory taxCategory) {
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

    public Money amount() {
        return amount;
    }

    public TaxCategory taxCategory() {
        return taxCategory;
    }
}
