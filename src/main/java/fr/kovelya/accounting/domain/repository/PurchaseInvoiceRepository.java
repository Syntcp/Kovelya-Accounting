package fr.kovelya.accounting.domain.repository;

import fr.kovelya.accounting.domain.purchase.PurchaseInvoice;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceId;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceStatus;
import fr.kovelya.accounting.domain.supplier.SupplierId;

import java.util.List;
import java.util.Optional;

public interface PurchaseInvoiceRepository {

    PurchaseInvoice save(PurchaseInvoice invoice);

    Optional<PurchaseInvoiceId> findIdByNumber(String number);

    Optional<PurchaseInvoice> findById(PurchaseInvoiceId id);

    List<PurchaseInvoice> findBySupplier(SupplierId supplierId);

    List<PurchaseInvoice> findAll();
}
