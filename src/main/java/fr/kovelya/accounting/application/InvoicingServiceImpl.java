package fr.kovelya.accounting.application;

import fr.kovelya.accounting.domain.customer.Customer;
import fr.kovelya.accounting.domain.customer.CustomerId;
import fr.kovelya.accounting.domain.invoice.InvoiceLine;
import fr.kovelya.accounting.domain.invoice.SalesInvoice;
import fr.kovelya.accounting.domain.repository.CustomerRepository;
import fr.kovelya.accounting.domain.repository.SalesInvoiceRepository;
import fr.kovelya.accounting.domain.shared.Money;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

public final class InvoicingServiceImpl implements InvoicingService {

    private final CustomerRepository customerRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final Currency defaultCurrency;

    public InvoicingServiceImpl(CustomerRepository customerRepository,
                                SalesInvoiceRepository salesInvoiceRepository,
                                Currency defaultCurrency) {
        this.customerRepository = customerRepository;
        this.salesInvoiceRepository = salesInvoiceRepository;
        this.defaultCurrency = defaultCurrency;
    }

    @Override
    public Customer createCustomer(String code, String name) {
        Customer customer = Customer.create(code, name);
        return customerRepository.save(customer);
    }

    @Override
    public List<Customer> listCustomers() {
        return customerRepository.findAll();
    }

    @Override
    public SalesInvoice createDraftInvoice(String number,
                                           CustomerId customerId,
                                           LocalDate issueDate,
                                           LocalDate dueDate,
                                           InvoiceLineRequest... lineRequests) {
        List<InvoiceLine> lines = new ArrayList<>();
        for (InvoiceLineRequest req : lineRequests) {
            Money amount = Money.of(req.amount(), defaultCurrency);
            lines.add(new InvoiceLine(req.description(), amount));
        }
        SalesInvoice invoice = SalesInvoice.draft(number, customerId, issueDate, dueDate, lines);
        return salesInvoiceRepository.save(invoice);
    }

    @Override
    public List<SalesInvoice> listInvoices() {
        return salesInvoiceRepository.findAll();
    }
}
