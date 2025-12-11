package fr.kovelya.accounting.domain.tax;

import java.math.BigDecimal;
import java.util.Objects;

public final class VatRate {

    private final BigDecimal value;

    public VatRate(BigDecimal value) {
        if(value == null) {
            throw new IllegalArgumentException("VAT rate is required");
        }
        if(value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("VAT rate must be between 0 and 1");
        }
        this.value = value;
    }

    public static VatRate ofFraction(BigDecimal fraction) {
        return new VatRate(fraction);
    }

    public BigDecimal value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VatRate vatRate)) return false;
        return Objects.equals(value, vatRate.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

}
