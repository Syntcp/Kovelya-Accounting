package fr.kovelya.accounting.domain.purchase;

import fr.kovelya.accounting.domain.ledger.LedgerId;
import fr.kovelya.accounting.domain.shared.Money;
import fr.kovelya.accounting.domain.supplier.SupplierId;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class PurchaseInvoice {

    private final PurchaseInvoiceId id;
    private final LedgerId ledgerId;
    private final String number;
    private final SupplierId supplierId;
    private final LocalDate issueDate;
    private final LocalDate dueDate;
    private final List<PurchaseInvoiceLine> lines;
    private final PurchaseInvoiceStatus status;

    public PurchaseInvoice(
            PurchaseInvoiceId id,
            LedgerId ledgerId,
            String number,
            SupplierId supplierId,
            LocalDate issueDate,
            LocalDate dueDate,
            List<PurchaseInvoiceLine> lines,
            PurchaseInvoiceStatus status
    ) {
        this.id = Objects.requireNonNull(id);
        if (ledgerId == null) {
            throw new IllegalArgumentException("Ledger is required");
        }
        if (number == null || number.isBlank()) {
            throw new IllegalArgumentException("Invoice number is required");
        }
        this.ledgerId = ledgerId;
        this.number = number;
        this.supplierId = Objects.requireNonNull(supplierId);
        this.issueDate = Objects.requireNonNull(issueDate);
        this.dueDate = Objects.requireNonNull(dueDate);
        this.lines = List.copyOf(lines);
        this.status = Objects.requireNonNull(status);
    }

    public static PurchaseInvoice draft(
            LedgerId ledgerId,
            String number,
            SupplierId supplierId,
            LocalDate issueDate,
            LocalDate dueDate,
            List<PurchaseInvoiceLine> lines
    ) {
        if (number == null || number.isBlank()) {
            throw new IllegalArgumentException("Invoice number is required");
        }
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("At least one line is required");
        }
        return new PurchaseInvoice(PurchaseInvoiceId.newId(), ledgerId, number, supplierId, issueDate, dueDate, lines, PurchaseInvoiceStatus.DRAFT);
    }

    public PurchaseInvoice issue() {
        if (status != PurchaseInvoiceStatus.DRAFT) {
            throw new IllegalStateException("Only draft purchase invoices can be issued");
        }
        return new PurchaseInvoice(id, ledgerId, number, supplierId, issueDate, dueDate, lines, PurchaseInvoiceStatus.ISSUED);
    }

    public PurchaseInvoice markPaid() {
        if (status != PurchaseInvoiceStatus.ISSUED && status != PurchaseInvoiceStatus.PARTIALLY_PAID) {
            throw new IllegalStateException("Only issued or partially paid purchase invoices can be marked as paid");
        }
        return new PurchaseInvoice(id, ledgerId, number, supplierId, issueDate, dueDate, lines, PurchaseInvoiceStatus.PAID);
    }

    public PurchaseInvoice markPartiallyPaid() {
        if (status != PurchaseInvoiceStatus.ISSUED && status != PurchaseInvoiceStatus.PARTIALLY_PAID) {
            throw new IllegalStateException("Only issued purchase invoices can become partially paid");
        }
        return new PurchaseInvoice(id, ledgerId, number, supplierId, issueDate, dueDate, lines, PurchaseInvoiceStatus.PARTIALLY_PAID);
    }

    public PurchaseInvoice cancel() {
        if (status == PurchaseInvoiceStatus.PAID) {
            throw new IllegalArgumentException("Paid purchase invoices cannot be cancelled");
        }
        if (status != PurchaseInvoiceStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft purchase invoices can be cancelled");
        }
        return new PurchaseInvoice(id, ledgerId, number, supplierId, issueDate, dueDate, lines, PurchaseInvoiceStatus.CANCELLED);
    }

    public PurchaseInvoiceId id() {
        return id;
    }

    public LedgerId ledgerId() {
        return ledgerId;
    }

    public String number() {
        return number;
    }

    public SupplierId supplierId() {
        return supplierId;
    }

    public LocalDate issueDate() {
        return issueDate;
    }

    public LocalDate dueDate() {
        return dueDate;
    }

    public List<PurchaseInvoiceLine> lines() {
        return Collections.unmodifiableList(lines);
    }

    public PurchaseInvoiceStatus status() {
        return status;
    }

    public Money total() {
        Money total = null;
        for (PurchaseInvoiceLine line : lines) {
            if (total == null) {
                total = line.amount();
            } else {
                total = total.add(line.amount());
            }
        }
        if (total == null) {
            throw new IllegalStateException("Invoice has no lines");
        }
        return total;
    }
}
