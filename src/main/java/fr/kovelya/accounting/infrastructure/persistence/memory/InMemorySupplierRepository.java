package fr.kovelya.accounting.infrastructure.persistence.memory;

import fr.kovelya.accounting.domain.repository.SupplierRepository;
import fr.kovelya.accounting.domain.supplier.Supplier;
import fr.kovelya.accounting.domain.supplier.SupplierId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class InMemorySupplierRepository implements SupplierRepository {

    private final List<Supplier> storage = new ArrayList<>();

    @Override
    public Supplier save(Supplier supplier) {
        for (int i = 0; i < storage.size(); i++) {
            if (storage.get(i).id().equals(supplier.id())) {
                storage.set(i, supplier);
                return supplier;
            }
        }
        storage.add(supplier);
        return supplier;
    }

    @Override
    public Optional<Supplier> findById(SupplierId id) {
        return storage.stream().filter(s -> s.id().equals(id)).findFirst();
    }

    @Override
    public Optional<Supplier> findByCode(String code) {
        return storage.stream().filter(s -> s.code().equals(code)).findFirst();
    }

    @Override
    public List<Supplier> findAll() {
        return new ArrayList<>(storage);
    }
}
