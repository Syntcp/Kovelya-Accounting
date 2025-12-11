package fr.kovelya.accounting.domain.invoice;

import fr.kovelya.accounting.domain.shared.Money;
import fr.kovelya.accounting.domain.tax.TaxCategory;

import java.util.Objects;

public final class InvoiceLine {

    private final String description;
    private final Money amount;
    private final TaxCategory taxCategory;

    public InvoiceLine(String description, Money amount, TaxCategory taxCategory) {
        if(description == null || description.isBlank()) {
            throw new IllegalArgumentException("Line description is required");
        }
        this.description = description;
        this.amount = Objects.requireNonNull(amount);

        // Exonéré = 0; donc on cherche toujours à avoir un chiffre/nombre ici.
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
