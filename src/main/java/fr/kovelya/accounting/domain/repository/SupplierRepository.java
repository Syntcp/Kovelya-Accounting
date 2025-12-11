package fr.kovelya.accounting.domain.repository;

import fr.kovelya.accounting.domain.supplier.Supplier;
import fr.kovelya.accounting.domain.supplier.SupplierId;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository {

    Supplier save(Supplier supplier);

    Optional<Supplier> findById(SupplierId id);

    Optional<Supplier> findByCode(String code);

    List<Supplier> findAll();
}
