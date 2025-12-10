package fr.kovelya.accounting.memory;

import fr.kovelya.accounting.domain.customer.CustomerId;
import fr.kovelya.accounting.domain.invoice.InvoiceStatus;
import fr.kovelya.accounting.domain.invoice.SalesInvoice;
import fr.kovelya.accounting.domain.invoice.SalesInvoiceId;
import fr.kovelya.accounting.domain.repository.SalesInvoiceRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemorySalesInvoiceRepository implements SalesInvoiceRepository {
    private final List<SalesInvoice> storage = new CopyOnWriteArrayList<>();

    @Override
    public SalesInvoice save(SalesInvoice invoice) {
        storage.add(invoice);
        return invoice;
    }

    @Override
    public Optional<SalesInvoice> findById(SalesInvoiceId id) {
        for (SalesInvoice invoice : storage) {
            if (invoice.id().equals(id)) {
                return Optional.of(invoice);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<SalesInvoice> findByNumber(String number) {
        for (SalesInvoice invoice : storage) {
            if (invoice.number().equals(number)) {
                return Optional.of(invoice);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<SalesInvoice> findByCustomer(CustomerId customerId) {
        List<SalesInvoice> result = new ArrayList<>();
        for (SalesInvoice invoice : storage) {
            if (invoice.customerId().equals(customerId)) {
                result.add(invoice);
            }
        }
        return result;
    }

    @Override
    public List<SalesInvoice> findByStatus(InvoiceStatus status) {
        List<SalesInvoice> result = new ArrayList<>();
        for (SalesInvoice invoice : storage) {
            if (invoice.status() == status) {
                result.add(invoice);
            }
        }
        return result;
    }

    @Override
    public List<SalesInvoice> findByIssueDateBetween(LocalDate from, LocalDate to) {
        List<SalesInvoice> result = new ArrayList<>();
        for (SalesInvoice invoice : storage) {
            LocalDate issue = invoice.issueDate();
            if ((issue.isEqual(from) || issue.isAfter(from)) &&
                    (issue.isEqual(to) || issue.isBefore(to))) {
                result.add(invoice);
            }
        }
        return result;
    }

    @Override
    public List<SalesInvoice> findAll() {
        return new ArrayList<>(storage);
    }
}