package fr.kovelya.accounting.application.service.impl;

import fr.kovelya.accounting.application.dto.InvoiceLineRequest;
import fr.kovelya.accounting.application.service.InvoicingService;
import fr.kovelya.accounting.domain.customer.Customer;
import fr.kovelya.accounting.domain.customer.CustomerId;
import fr.kovelya.accounting.domain.invoice.InvoiceLine;
import fr.kovelya.accounting.domain.invoice.InvoiceStatus;
import fr.kovelya.accounting.domain.invoice.SalesInvoice;
import fr.kovelya.accounting.domain.invoice.SalesInvoiceId;
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

    public InvoicingServiceImpl(CustomerRepository customerRepository, SalesInvoiceRepository salesInvoiceRepository, Currency defaultCurrency) {
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
    public SalesInvoice createDraftInvoice(String number, CustomerId customerId, LocalDate issueDate, LocalDate dueDate, InvoiceLineRequest... lineRequests) {
        List<InvoiceLine> lines = new ArrayList<>();
        for (InvoiceLineRequest req : lineRequests) {
            Money amount = Money.of(req.amount(), defaultCurrency);
            lines.add(new InvoiceLine(req.description(), amount, req.taxCategory()));
        }
        SalesInvoice invoice = SalesInvoice.draft(number, customerId, issueDate, dueDate, lines);
        return salesInvoiceRepository.save(invoice);
    }

    @Override
    public SalesInvoice createCreditNoteFromInvoice(String creditNoteNumber, SalesInvoiceId originalInvoiceId, LocalDate issueDate, LocalDate dueDate) {
        SalesInvoice original = salesInvoiceRepository.findById(originalInvoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Original invoice not found"));

        if (original.status() == InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Cannot create credit from draft invoice");
        }

        if (original.status() == InvoiceStatus.CANCELLED) {
            throw new IllegalStateException("Cannot create credit note from cancelled invoice");
        }

        if (dueDate.isBefore(issueDate)) {
            throw new IllegalArgumentException("Due date must be on or after issue date");
        }

        List<InvoiceLine> creditLines = new ArrayList<>();
        for (InvoiceLine line : original.lines()) {
            Money negAmount = line.amount().negate();
            creditLines.add(new InvoiceLine(line.description(), negAmount, line.taxCategory()));
        }

        SalesInvoice creditNote = SalesInvoice.draft(
                creditNoteNumber,
                original.customerId(),
                issueDate,
                dueDate,
                creditLines
        );
        return salesInvoiceRepository.save(creditNote);
    }

    @Override
    public List<SalesInvoice> listInvoices() {
        return salesInvoiceRepository.findAll();
    }
}
