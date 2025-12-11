package fr.kovelya.accounting.domain.purchase;

import fr.kovelya.accounting.domain.shared.Money;
import fr.kovelya.accounting.domain.supplier.SupplierId;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class PurchaseInvoice {

    private final PurchaseInvoiceId id;
    private final String number;
    private final SupplierId supplierId;
    private final LocalDate issueDate;
    private final LocalDate dueDate;
    private final List<PurchaseInvoiceLine> lines;
    private final PurchaseInvoiceStatus status;

    public PurchaseInvoice(PurchaseInvoiceId id, String number, SupplierId supplierId, LocalDate issueDate, LocalDate dueDate, List<PurchaseInvoiceLine> lines, PurchaseInvoiceStatus status) {
        this.id = Objects.requireNonNull(id);
        this.number = Objects.requireNonNull(number);
        this.supplierId = Objects.requireNonNull(supplierId);
        this.issueDate = Objects.requireNonNull(issueDate);
        this.dueDate = Objects.requireNonNull(dueDate);
        this.lines = List.copyOf(lines);
        this.status = Objects.requireNonNull(status);
    }

    public static PurchaseInvoice draft(String number, SupplierId supplierId, LocalDate issueDate, LocalDate dueDate, List<PurchaseInvoiceLine> lines) {
        if (number == null || number.isBlank()) {
            throw new IllegalArgumentException("Invoice number is required");
        }
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("At least one line is required");
        }

        return new PurchaseInvoice(
                PurchaseInvoiceId.newId(),
                number,
                supplierId,
                issueDate,
                dueDate,
                new ArrayList<>(lines),
                PurchaseInvoiceStatus.DRAFT
        );
    }

    public PurchaseInvoice issue() {
        if (status != PurchaseInvoiceStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft invoices can be issued");
        }
        return new PurchaseInvoice(id, number, supplierId, issueDate, dueDate, lines, PurchaseInvoiceStatus.ISSUED);
    }

    public PurchaseInvoice markPaid() {
        if (status != PurchaseInvoiceStatus.ISSUED) {
            throw new IllegalArgumentException("Only issued invoices can be paid");
        }
        return new PurchaseInvoice(id, number, supplierId, issueDate, dueDate, lines, PurchaseInvoiceStatus.PAID);
    }

    public PurchaseInvoiceId id() {
        return id;
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
