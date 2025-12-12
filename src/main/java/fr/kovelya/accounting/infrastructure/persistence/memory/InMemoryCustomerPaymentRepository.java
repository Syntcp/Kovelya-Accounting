package fr.kovelya.accounting.infrastructure.persistence.memory;

import fr.kovelya.accounting.domain.invoice.SalesInvoiceId;
import fr.kovelya.accounting.domain.payment.CustomerPayment;
import fr.kovelya.accounting.domain.payment.CustomerPaymentId;
import fr.kovelya.accounting.domain.repository.CustomerPaymentRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemoryCustomerPaymentRepository implements CustomerPaymentRepository {

    private final List<CustomerPayment> storage = new CopyOnWriteArrayList<>();

    @Override
    public CustomerPayment save(CustomerPayment payment) {
        for (int i =0; i < storage.size(); i++) {
            if (storage.get(i).id().equals(payment.id())) {
                storage.set(i, payment);
                        return payment;
            }
        }
        storage.add(payment);
        return payment;
    }

    @Override
    public List<CustomerPayment> findByInvoice(SalesInvoiceId invoiceId) {
        List<CustomerPayment> result = new ArrayList<>();
        for (CustomerPayment payment : storage) {
            if (payment.invoiceId().equals(invoiceId)) {
                result.add(payment);
            }
        }
        return result;
    }

    @Override
    public List<CustomerPayment> findAll() {
        return new ArrayList<>(storage);
    }

    @Override
    public Optional<CustomerPayment> findById(CustomerPaymentId id) {
        for (CustomerPayment payment : storage) {
            if (payment.id().equals(id)) {
                return Optional.of(payment);
            }
        }
        return Optional.empty();
    }
}
