package fr.kovelya.accounting.infrastructure.persistence.memory;

import fr.kovelya.accounting.domain.purchase.PurchaseInvoice;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceId;
import fr.kovelya.accounting.domain.repository.PurchaseInvoiceRepository;
import fr.kovelya.accounting.domain.supplier.SupplierId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class InMemoryPurchaseInvoiceRepository implements PurchaseInvoiceRepository {

    private final List<PurchaseInvoice> storage = new ArrayList<>();

    @Override
    public PurchaseInvoice save(PurchaseInvoice invoice) {
        for (int i = 0; i < storage.size(); i++) {
            if (storage.get(i).id().equals(invoice.id())) {
                storage.set(i, invoice);
                return invoice;
            }
        }
        storage.add(invoice);
        return invoice;
    }

    @Override
    public Optional<PurchaseInvoiceId> findIdByNumber(String number) {
        return storage.stream()
                .filter(inv -> inv.number().equals(number))
                .map(PurchaseInvoice::id)
                .findFirst();
    }

    @Override
    public Optional<PurchaseInvoice> findById(PurchaseInvoiceId id) {
        return storage.stream().filter(inv -> inv.id().equals(id)).findFirst();
    }

    @Override
    public List<PurchaseInvoice> findBySupplier(SupplierId supplierId) {
        List<PurchaseInvoice> result = new ArrayList<>();
        for (PurchaseInvoice invoice : storage) {
            if (invoice.supplierId().equals(supplierId)) {
                result.add(invoice);
            }
        }
        return result;
    }

    @Override
    public List<PurchaseInvoice> findAll() {
        return new ArrayList<>(storage);
    }
}
