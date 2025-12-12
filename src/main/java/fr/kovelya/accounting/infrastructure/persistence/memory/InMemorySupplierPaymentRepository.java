package fr.kovelya.accounting.infrastructure.persistence.memory;

import fr.kovelya.accounting.domain.payment.SupplierPayment;
import fr.kovelya.accounting.domain.payment.SupplierPaymentId;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceId;
import fr.kovelya.accounting.domain.repository.SupplierPaymentRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemorySupplierPaymentRepository implements SupplierPaymentRepository {

    private final List<SupplierPayment> storage = new CopyOnWriteArrayList<>();

    @Override
    public SupplierPayment save(SupplierPayment payment) {
        for (int i = 0; i < storage.size(); i++) {
            if (storage.get(i).id().equals(payment.id())) {
                storage.set(i, payment);
                return payment;
            }
        }
        storage.add(payment);
        return payment;
    }

    @Override
    public List<SupplierPayment> findByInvoice(PurchaseInvoiceId invoiceId) {
        List<SupplierPayment> result = new ArrayList<>();
        for (SupplierPayment payment : storage) {
            if (payment.invoiceId().equals(invoiceId)) {
                result.add(payment);
            }
        }
        return result;
    }

    @Override
    public List<SupplierPayment> findAll() {
        return new ArrayList<>(storage);
    }

    @Override
    public Optional<SupplierPayment> findById(SupplierPaymentId id) {
        for (SupplierPayment payment : storage) {
            if (payment.id().equals(id)) {
                return Optional.of(payment);
            }
        }
        return Optional.empty();
    }
}
