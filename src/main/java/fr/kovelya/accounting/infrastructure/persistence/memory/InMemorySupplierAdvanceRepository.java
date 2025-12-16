package fr.kovelya.accounting.infrastructure.persistence.memory;

import fr.kovelya.accounting.domain.advance.SupplierAdvance;
import fr.kovelya.accounting.domain.repository.SupplierAdvanceRepository;
import fr.kovelya.accounting.domain.supplier.SupplierId;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemorySupplierAdvanceRepository implements SupplierAdvanceRepository {

    private final List<SupplierAdvance> storage = new CopyOnWriteArrayList<>();

    @Override
    public SupplierAdvance save(SupplierAdvance advance) {
        for (int i = 0; i < storage.size(); i++) {
            if (storage.get(i).id().equals(advance.id())) {
                storage.set(i, advance);
                return advance;
            }
        }
        storage.add(advance);
        return advance;
    }

    @Override
    public List<SupplierAdvance> findOpenBySupplier(SupplierId supplierId) {
        List<SupplierAdvance> result = new ArrayList<>();
        for (SupplierAdvance a : storage) {
            if (a.supplierId().equals(supplierId) && a.remaining().amount().compareTo(BigDecimal.ZERO) > 0) {
                result.add(a);
            }
        }
        return result;
    }

    @Override
    public Optional<SupplierAdvance> findBySourceCommandId(UUID commandId) {
        for (SupplierAdvance a : storage) {
            if (a.sourceCommandId().equals(commandId)) {
                return Optional.of(a);
            }
        }
        return Optional.empty();
    }
}
