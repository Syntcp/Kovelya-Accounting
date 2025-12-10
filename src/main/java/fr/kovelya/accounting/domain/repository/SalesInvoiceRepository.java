package fr.kovelya.accounting.domain.repository;

import fr.kovelya.accounting.domain.customer.CustomerId;
import fr.kovelya.accounting.domain.invoice.InvoiceStatus;
import fr.kovelya.accounting.domain.invoice.SalesInvoice;
import fr.kovelya.accounting.domain.invoice.SalesInvoiceId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SalesInvoiceRepository {
    SalesInvoice save(SalesInvoice invoice);

    Optional<SalesInvoice> findById(SalesInvoiceId id);

    Optional<SalesInvoice> findByNumber(String number);

    List<SalesInvoice> findByCustomer(CustomerId customerId);

    List<SalesInvoice> findByStatus(InvoiceStatus status);

    List<SalesInvoice> findByIssueDateBetween(LocalDate from, LocalDate to);

    List<SalesInvoice> findAll();
}
