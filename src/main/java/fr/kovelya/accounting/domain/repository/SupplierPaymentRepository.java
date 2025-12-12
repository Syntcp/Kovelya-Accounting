package fr.kovelya.accounting.domain.repository;

import fr.kovelya.accounting.domain.payment.SupplierPayment;
import fr.kovelya.accounting.domain.payment.SupplierPaymentId;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceId;

import java.util.List;
import java.util.Optional;

public interface SupplierPaymentRepository {

    SupplierPayment save(SupplierPayment payment);

    List<SupplierPayment> findByInvoice(PurchaseInvoiceId invoiceId);

    List<SupplierPayment> findAll();

    Optional<SupplierPayment> findById(SupplierPaymentId id);
}
