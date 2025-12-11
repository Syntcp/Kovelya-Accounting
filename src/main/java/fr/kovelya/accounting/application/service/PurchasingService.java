package fr.kovelya.accounting.application.service;

import fr.kovelya.accounting.application.dto.PurchaseInvoiceLineRequest;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoice;
import fr.kovelya.accounting.domain.supplier.Supplier;

import java.time.LocalDate;
import java.util.List;

public interface PurchasingService {

    Supplier createSupplier(String code, String name);

    List<Supplier> listSuppliers();

    PurchaseInvoice createDraftPurchaseInvoice(String number, Supplier supplier, LocalDate issueDate, LocalDate dueDate, PurchaseInvoiceLineRequest... lineRequests);

    List<PurchaseInvoice> listPurchaseInvoices();
}
