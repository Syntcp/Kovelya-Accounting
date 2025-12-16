package fr.kovelya.accounting.domain.repository;

import fr.kovelya.accounting.domain.advance.SupplierAdvance;
import fr.kovelya.accounting.domain.supplier.SupplierId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierAdvanceRepository {

    SupplierAdvance save(SupplierAdvance advance);

    List<SupplierAdvance> findOpenBySupplier(SupplierId supplierId);

    Optional<SupplierAdvance> findBySourceCommandId(UUID commandId);
}
