package fr.kovelya.accounting.application.service.impl;

import fr.kovelya.accounting.application.dto.PurchaseInvoiceLineRequest;
import fr.kovelya.accounting.application.service.PurchasingService;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoice;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceLine;
import fr.kovelya.accounting.domain.repository.PurchaseInvoiceRepository;
import fr.kovelya.accounting.domain.repository.SupplierRepository;
import fr.kovelya.accounting.domain.shared.Money;
import fr.kovelya.accounting.domain.supplier.Supplier;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

public final class PurchasingServiceImpl implements PurchasingService {

    private final SupplierRepository supplierRepository;
    private final PurchaseInvoiceRepository purchaseInvoiceRepository;
    private final Currency defaultCurrency;

    public PurchasingServiceImpl(SupplierRepository supplierRepository, PurchaseInvoiceRepository purchaseInvoiceRepository, Currency defaultCurrency) {
        this.supplierRepository = supplierRepository;
        this.purchaseInvoiceRepository = purchaseInvoiceRepository;
        this.defaultCurrency = defaultCurrency;
    }

    @Override
    public Supplier createSupplier(String code, String name) {
        Supplier supplier = Supplier.create(code, name);
        return supplierRepository.save(supplier);
    }

    @Override
    public List<Supplier> listSuppliers() {
        return supplierRepository.findAll();
    }

    @Override
    public PurchaseInvoice createDraftPurchaseInvoice(String number, Supplier supplier, LocalDate issueDate, LocalDate dueDate, PurchaseInvoiceLineRequest... lineRequests) {
        List<PurchaseInvoiceLine> lines = new ArrayList<>();
        for (PurchaseInvoiceLineRequest req : lineRequests) {
            Money amount = Money.of(req.amount(), defaultCurrency);
            lines.add(new PurchaseInvoiceLine(req.description(), amount, req.taxCategory()));
        }
        PurchaseInvoice invoice = PurchaseInvoice.draft(
                number,
                supplier.id(),
                issueDate,
                dueDate,
                lines
        );
        return purchaseInvoiceRepository.save(invoice);
    }

    @Override
    public List<PurchaseInvoice> listPurchaseInvoices() {
        return purchaseInvoiceRepository.findAll();
    }
}
