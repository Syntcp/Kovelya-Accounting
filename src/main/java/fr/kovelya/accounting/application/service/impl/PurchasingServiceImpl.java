package fr.kovelya.accounting.application.service.impl;

import fr.kovelya.accounting.application.dto.PurchaseInvoiceLineRequest;
import fr.kovelya.accounting.application.service.PurchasingService;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoice;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceId;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceLine;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceStatus;
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
    public PurchaseInvoice createPurchaseCreditNoteFromInvoice(String creditNoteNumber, PurchaseInvoiceId originalInvoiceId, LocalDate issueDate, LocalDate dueDate) {
        PurchaseInvoice original = purchaseInvoiceRepository.findById(originalInvoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Original purchase invoice not found"));

        if (original.status() == PurchaseInvoiceStatus.DRAFT) {
            throw new IllegalStateException("Cannot create credit note from draft purchase invoice");
        }

        if (original.status() == PurchaseInvoiceStatus.CANCELLED) {
            throw new IllegalStateException("Cannot create credit note from cancelled purchase invoice");
        }

        if (dueDate.isBefore(issueDate)) {
            throw new IllegalArgumentException("Due date must be on or after issue date");
        }

        List<PurchaseInvoiceLine> creditLines = new ArrayList<>();
        for (PurchaseInvoiceLine line : original.lines()) {
            Money negAmount = line.amount().negate();
            creditLines.add(new PurchaseInvoiceLine(line.description(), negAmount, line.taxCategory()));
        }

        PurchaseInvoice creditNote = PurchaseInvoice.draft(
                creditNoteNumber,
                original.supplierId(),
                issueDate,
                dueDate,
                creditLines
        );
        return purchaseInvoiceRepository.save(creditNote);
    }

    @Override
    public List<PurchaseInvoice> listPurchaseInvoices() {
        return purchaseInvoiceRepository.findAll();
    }
}
