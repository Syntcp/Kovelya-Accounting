package fr.kovelya.accounting.application.service;

import fr.kovelya.accounting.application.dto.InvoiceLineRequest;
import fr.kovelya.accounting.domain.customer.Customer;
import fr.kovelya.accounting.domain.customer.CustomerId;
import fr.kovelya.accounting.domain.invoice.SalesInvoice;
import fr.kovelya.accounting.domain.invoice.SalesInvoiceId;

import java.time.LocalDate;
import java.util.List;

public interface InvoicingService {

    Customer createCustomer(String code, String name);

    List<Customer> listCustomers();

    SalesInvoice createDraftInvoice(String number, CustomerId customerId, LocalDate issueDate, LocalDate dueDate, InvoiceLineRequest... lines);

    SalesInvoice createCreditNoteFromInvoice(String creditNoteNumber, SalesInvoiceId originalInvoiceId, LocalDate issueDate, LocalDate dueDate);

    List<SalesInvoice> listInvoices();
}
