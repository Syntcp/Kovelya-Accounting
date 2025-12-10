package fr.kovelya.accounting.application;

import fr.kovelya.accounting.domain.customer.Customer;
import fr.kovelya.accounting.domain.customer.CustomerId;
import fr.kovelya.accounting.domain.invoice.SalesInvoice;

import java.time.LocalDate;
import java.util.List;

public interface InvoicingService {

    Customer createCustomer(String code, String name);

    List<Customer> listCustomers();

    SalesInvoice createDraftInvoice(String number,
                                    CustomerId customerId,
                                    LocalDate issueDate,
                                    LocalDate dueDate,
                                    InvoiceLineRequest... lines);

    List<SalesInvoice> listInvoices();
}
