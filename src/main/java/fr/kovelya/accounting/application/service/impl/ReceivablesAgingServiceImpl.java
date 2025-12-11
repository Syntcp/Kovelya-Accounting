package fr.kovelya.accounting.application.service.impl;

import fr.kovelya.accounting.application.report.CustomerReceivableAgingView;
import fr.kovelya.accounting.application.service.ReceivablesAgingService;
import fr.kovelya.accounting.domain.customer.Customer;
import fr.kovelya.accounting.domain.customer.CustomerId;
import fr.kovelya.accounting.domain.invoice.InvoiceStatus;
import fr.kovelya.accounting.domain.invoice.SalesInvoice;
import fr.kovelya.accounting.domain.repository.CustomerRepository;
import fr.kovelya.accounting.domain.repository.SalesInvoiceRepository;
import fr.kovelya.accounting.domain.shared.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public final class ReceivablesAgingServiceImpl implements ReceivablesAgingService {

    private final CustomerRepository customerRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;

    public ReceivablesAgingServiceImpl(CustomerRepository customerRepository,
                                       SalesInvoiceRepository salesInvoiceRepository) {
        this.customerRepository = customerRepository;
        this.salesInvoiceRepository = salesInvoiceRepository;
    }

    @Override
    public List<CustomerReceivableAgingView> getCustomerAging(LocalDate asOfDate) {
        List<CustomerReceivableAgingView> result = new ArrayList<>();

        List<Customer> customers = customerRepository.findAll();
        for (Customer customer : customers) {
            CustomerReceivableAgingView view = computeForCustomer(customer, asOfDate);
            if (view != null && view.total().amount().compareTo(BigDecimal.ZERO) > 0) {
                result.add(view);
            }
        }

        return result;
    }

    private CustomerReceivableAgingView computeForCustomer(Customer customer, LocalDate asOfDate) {
        CustomerId customerId = customer.id();
        List<SalesInvoice> invoices = salesInvoiceRepository.findByCustomer(customerId);

        Money notDue = null;
        Money due0_30 = null;
        Money due31_60 = null;
        Money due61_90 = null;
        Money due90Plus = null;

        for (SalesInvoice invoice : invoices) {
            if (invoice.status() == InvoiceStatus.PAID || invoice.status() == InvoiceStatus.CANCELLED) {
                continue;
            }

            Money amount = invoice.total();

            if (notDue == null) {
                notDue = Money.zero(amount.currency());
                due0_30 = Money.zero(amount.currency());
                due31_60 = Money.zero(amount.currency());
                due61_90 = Money.zero(amount.currency());
                due90Plus = Money.zero(amount.currency());
            }

            long daysPastDue = ChronoUnit.DAYS.between(invoice.dueDate(), asOfDate);

            if (daysPastDue < 0) {
                notDue = notDue.add(amount);
            } else if (daysPastDue <= 30) {
                due0_30 = due0_30.add(amount);
            } else if (daysPastDue <= 60) {
                due31_60 = due31_60.add(amount);
            } else if (daysPastDue <= 90) {
                due61_90 = due61_90.add(amount);
            } else {
                due90Plus = due90Plus.add(amount);
            }
        }

        if (notDue == null) {
            return null;
        }

        Money total = notDue
                .add(due0_30)
                .add(due31_60)
                .add(due61_90)
                .add(due90Plus);

        if (total.amount().compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return new CustomerReceivableAgingView(
                customer,
                notDue,
                due0_30,
                due31_60,
                due61_90,
                due90Plus,
                total
        );
    }
}
