package fr.kovelya.accounting.application.service.impl;

import fr.kovelya.accounting.application.dto.InvoiceLineRequest;
import fr.kovelya.accounting.application.service.InvoicingService;
import fr.kovelya.accounting.domain.customer.Customer;
import fr.kovelya.accounting.domain.customer.CustomerId;
import fr.kovelya.accounting.domain.invoice.InvoiceLine;
import fr.kovelya.accounting.domain.invoice.InvoiceStatus;
import fr.kovelya.accounting.domain.invoice.SalesInvoice;
import fr.kovelya.accounting.domain.invoice.SalesInvoiceId;
import fr.kovelya.accounting.domain.ledger.LedgerId;
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

    public InvoicingServiceImpl(CustomerRepository customerRepository, SalesInvoiceRepository salesInvoiceRepository, Currency defaultCurrency
    ) {
        this.customerRepository = customerRepository;
        this.salesInvoiceRepository = salesInvoiceRepository;
        this.defaultCurrency = defaultCurrency;
    }

    @Override
    public Customer createCustomer(LedgerId ledgerId, String code, String name) {
        Customer customer = Customer.create(ledgerId, code, name);
        return customerRepository.save(customer);
    }

    @Override
    public List<Customer> listCustomers(LedgerId ledgerId) {
        List<Customer> result = new ArrayList<>();
        for (Customer customer : customerRepository.findAll()) {
            if (customer.ledgerId().equals(ledgerId)) {
                result.add(customer);
            }
        }
        return result;
    }

    @Override
    public SalesInvoice createDraftInvoice(LedgerId ledgerId, String number, CustomerId customerId, LocalDate issueDate, LocalDate dueDate, InvoiceLineRequest... lineRequests
    ) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        if (!customer.ledgerId().equals(ledgerId)) {
            throw new IllegalArgumentException("Customer does not belong to the given ledger");
        }

        List<InvoiceLine> lines = new ArrayList<>();
        for (InvoiceLineRequest req : lineRequests) {
            Money amount = Money.of(req.amount(), defaultCurrency);
            lines.add(new InvoiceLine(req.description(), amount, req.taxCategory()));
        }

        SalesInvoice invoice = SalesInvoice.draft(ledgerId, number, customerId, issueDate, dueDate, lines);
        return salesInvoiceRepository.save(invoice);
    }

    @Override
    public SalesInvoice createCreditNoteFromInvoice(LedgerId ledgerId, String creditNoteNumber, SalesInvoiceId originalInvoiceId, LocalDate issueDate, LocalDate dueDate
    ) {
        SalesInvoice original = salesInvoiceRepository.findById(originalInvoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Original invoice not found"));

        if (!original.ledgerId().equals(ledgerId)) {
            throw new IllegalArgumentException("Original invoice does not belong to the given ledger");
        }

        if (original.status() != InvoiceStatus.ISSUED && original.status() != InvoiceStatus.PAID && original.status() != InvoiceStatus.PARTIALLY_PAID) {
            throw new IllegalStateException("Only issued or paid invoices can be credited");
        }

        List<InvoiceLine> creditLines = new ArrayList<>();
        for (InvoiceLine line : original.lines()) {
            Money negAmount = Money.of(line.amount().amount().negate(), line.amount().currency());
            creditLines.add(new InvoiceLine(line.description(), negAmount, line.taxCategory()));
        }

        SalesInvoice creditNote = SalesInvoice.draft(
                ledgerId,
                creditNoteNumber,
                original.customerId(),
                issueDate,
                dueDate,
                creditLines
        );
        return salesInvoiceRepository.save(creditNote);
    }

    @Override
    public List<SalesInvoice> listInvoices(LedgerId ledgerId) {
        List<SalesInvoice> result = new ArrayList<>();
        for (SalesInvoice invoice : salesInvoiceRepository.findAll()) {
            if (invoice.ledgerId().equals(ledgerId)) {
                result.add(invoice);
            }
        }
        return result;
    }
}
