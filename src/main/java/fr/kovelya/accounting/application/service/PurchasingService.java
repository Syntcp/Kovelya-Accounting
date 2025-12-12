package fr.kovelya.accounting.application.service;

import fr.kovelya.accounting.application.dto.PurchaseInvoiceLineRequest;
import fr.kovelya.accounting.domain.ledger.LedgerId;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoice;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceId;
import fr.kovelya.accounting.domain.supplier.Supplier;
import fr.kovelya.accounting.domain.supplier.SupplierId;

import java.time.LocalDate;
import java.util.List;

public interface PurchasingService {

    Supplier createSupplier(LedgerId ledgerId, String code, String name);

    List<Supplier> listSuppliers(LedgerId ledgerId);

    PurchaseInvoice createDraftPurchaseInvoice(LedgerId ledgerId, String number, SupplierId supplierId, LocalDate issueDate, LocalDate dueDate, PurchaseInvoiceLineRequest... lineRequests
    );

    PurchaseInvoice createPurchaseCreditNoteFromInvoice(LedgerId ledgerId, String creditNoteNumber, PurchaseInvoiceId originalInvoiceId, LocalDate issueDate, LocalDate dueDate
    );

    List<PurchaseInvoice> listPurchaseInvoices(LedgerId ledgerId);
}
