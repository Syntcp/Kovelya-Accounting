package fr.kovelya.accounting.domain.invoice;

import fr.kovelya.accounting.domain.customer.CustomerId;
import fr.kovelya.accounting.domain.shared.Money;

import java.time.LocalDate;
import java.util.List;

public final class SalesInvoice {

    private final SalesInvoiceId id;
    private final String number;
    private final CustomerId customerId;
    private final LocalDate issueDate;
    private final LocalDate dueDate;
    private final List<InvoiceLine> lines;
    private final InvoiceStatus status;

    public SalesInvoice(SalesInvoiceId id, String number, CustomerId customerId, LocalDate issueDate, LocalDate dueDate, List<InvoiceLine> lines, InvoiceStatus status) {
        if (number == null || number.isBlank()) {
            throw new IllegalArgumentException("Invoice number is required");
        }
        if (customerId == null) {
            throw new IllegalArgumentException("Customer is required");
        }
        if (issueDate == null || dueDate == null) {
            throw new IllegalArgumentException("Issue and due dates are required");
        }
        if (dueDate.isBefore(issueDate)) {
            throw new IllegalArgumentException("Due date must be on or after issue date");
        }
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Invoice must have at least one line");
        }
        this.id = id;
        this.number = number;
        this.customerId = customerId;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.lines = List.copyOf(lines);
        this.status = status;
    }

    public static SalesInvoice draft(String number, CustomerId customerId, LocalDate issueDate, LocalDate dueDate, List<InvoiceLine> lines) {
        return new SalesInvoice(SalesInvoiceId.newId(), number, customerId, issueDate, dueDate, lines, InvoiceStatus.DRAFT);
    }

    public SalesInvoice issue() {
        if (status != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Only draft invoices can be issued");
        }
        return new SalesInvoice(id, number, customerId, issueDate, dueDate, lines, InvoiceStatus.ISSUED);
    }

    public SalesInvoice markPaid() {
        if (status != InvoiceStatus.ISSUED) {
            throw new IllegalStateException("Only issued invoices can be marked as paid");
        }
        return new SalesInvoice(id, number, customerId, issueDate, dueDate, lines, InvoiceStatus.PAID);
    }

    public SalesInvoice cancel() {
        if (status == InvoiceStatus.PAID) {
            throw new IllegalStateException("Paid invoices cannot be cancelled");
        }
        if (status != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Only draft invoices can be cancelled; use a credit not for issued invoices");
        }
        return new SalesInvoice(id, number, customerId, issueDate, dueDate, lines, InvoiceStatus.CANCELLED);
    }


    public SalesInvoiceId id() {
        return id;
    }

    public String number() {
        return number;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public LocalDate issueDate() {
        return issueDate;
    }

    public LocalDate dueDate() {
        return dueDate;
    }

    public List<InvoiceLine> lines() {
        return List.copyOf(lines);
    }

    public InvoiceStatus status() {
        return status;
    }

    public Money total() {
        Money result = lines.get(0).amount();
        for (int i = 1; i < lines.size(); i++) {
            result = result.add(lines.get(i).amount());
        }
        return result;
    }
}
