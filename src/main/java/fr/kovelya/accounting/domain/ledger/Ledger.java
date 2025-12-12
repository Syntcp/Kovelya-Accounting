package fr.kovelya.accounting.domain.ledger;

import java.util.Currency;
import java.util.Objects;

public final class Ledger {

    private final LedgerId id;
    private final String code;
    private final String name;
    private final Currency baseCurrency;

    public Ledger(LedgerId id, String code, String name, Currency baseCurrency) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Ledger code is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Ledger name is required");
        }
        if (baseCurrency == null) {
            throw new IllegalArgumentException("Ledger currency is required");
        }
        this.id = id;
        this.code = code;
        this.name = name;
        this.baseCurrency = baseCurrency;
    }

    public static Ledger create(String code, String name, Currency baseCurrency) {
        return new Ledger(LedgerId.newId(), code, name, baseCurrency);
    }

    public LedgerId id() {
        return id;
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }

    public Currency baseCurrency() {
        return baseCurrency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ledger ledger)) return false;
        return Objects.equals(id, ledger.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
