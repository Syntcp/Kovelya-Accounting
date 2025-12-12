package fr.kovelya.accounting.domain.period;

import fr.kovelya.accounting.domain.ledger.Ledger;
import fr.kovelya.accounting.domain.ledger.LedgerId;

import java.time.LocalDate;
import java.util.Objects;

public final class AccountingPeriod {

    private final AccountingPeriodId id;
    private final LedgerId ledgerId;
    private final String name;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final PeriodStatus status;

    public AccountingPeriod(AccountingPeriodId id, LedgerId ledgerId, String name, LocalDate startDate, LocalDate endDate, PeriodStatus status) {
        if (ledgerId == null) {
            throw new IllegalArgumentException("Ledger is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Period name is required");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start and end dates are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be on or after start date");
        }
        this.id = id;
        this.ledgerId = ledgerId;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public static AccountingPeriod open(LedgerId ledgerId, String name, LocalDate startDate, LocalDate endDate) {
        return new AccountingPeriod(AccountingPeriodId.newId(), ledgerId, name, startDate, endDate, PeriodStatus.OPEN);
    }

    public AccountingPeriodId id() {
        return id;
    }

    public LedgerId ledgerId() {
        return ledgerId;
    }

    public String name() {
        return name;
    }

    public LocalDate startDate() {
        return startDate;
    }

    public LocalDate endDate() {
        return endDate;
    }

    public PeriodStatus status() {
        return status;
    }

    public AccountingPeriod close() {
        return new AccountingPeriod(id, ledgerId, name, startDate, endDate, PeriodStatus.CLOSED);
    }

    public AccountingPeriod archive() {
        return new AccountingPeriod(id, ledgerId, name, startDate, endDate, PeriodStatus.ARCHIVED);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccountingPeriod that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
