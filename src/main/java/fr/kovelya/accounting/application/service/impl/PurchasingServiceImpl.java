package fr.kovelya.accounting.application.service.impl;

import fr.kovelya.accounting.application.dto.PurchaseInvoiceLineRequest;
import fr.kovelya.accounting.application.service.PurchasingService;
import fr.kovelya.accounting.domain.ledger.LedgerId;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoice;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceId;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceLine;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceStatus;
import fr.kovelya.accounting.domain.repository.PurchaseInvoiceRepository;
import fr.kovelya.accounting.domain.repository.SupplierRepository;
import fr.kovelya.accounting.domain.shared.Money;
import fr.kovelya.accounting.domain.supplier.Supplier;
import fr.kovelya.accounting.domain.supplier.SupplierId;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

public final class PurchasingServiceImpl implements PurchasingService {

    private final SupplierRepository supplierRepository;
    private final PurchaseInvoiceRepository purchaseInvoiceRepository;
    private final Currency defaultCurrency;

    public PurchasingServiceImpl(SupplierRepository supplierRepository, PurchaseInvoiceRepository purchaseInvoiceRepository, Currency defaultCurrency
    ) {
        this.supplierRepository = supplierRepository;
        this.purchaseInvoiceRepository = purchaseInvoiceRepository;
        this.defaultCurrency = defaultCurrency;
    }

    @Override
    public Supplier createSupplier(LedgerId ledgerId, String code, String name) {
        Supplier supplier = Supplier.create(ledgerId, code, name);
        return supplierRepository.save(supplier);
    }

    @Override
    public List<Supplier> listSuppliers(LedgerId ledgerId) {
        List<Supplier> result = new ArrayList<>();
        for (Supplier supplier : supplierRepository.findAll()) {
            if (supplier.ledgerId().equals(ledgerId)) {
                result.add(supplier);
            }
        }
        return result;
    }

    @Override
    public PurchaseInvoice createDraftPurchaseInvoice(LedgerId ledgerId, String number, SupplierId supplierId, LocalDate issueDate, LocalDate dueDate, PurchaseInvoiceLineRequest... lineRequests
    ) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + supplierId));
        if (!supplier.ledgerId().equals(ledgerId)) {
            throw new IllegalArgumentException("Supplier does not belong to the given ledger");
        }

        List<PurchaseInvoiceLine> lines = new ArrayList<>();
        for (PurchaseInvoiceLineRequest req : lineRequests) {
            Money amount = Money.of(req.amount(), defaultCurrency);
            lines.add(new PurchaseInvoiceLine(req.description(), amount, req.taxCategory()));
        }

        PurchaseInvoice invoice = PurchaseInvoice.draft(
                ledgerId,
                number,
                supplierId,
                issueDate,
                dueDate,
                lines
        );
        return purchaseInvoiceRepository.save(invoice);
    }

    @Override
    public PurchaseInvoice createPurchaseCreditNoteFromInvoice(LedgerId ledgerId, String creditNoteNumber, PurchaseInvoiceId originalInvoiceId, LocalDate issueDate, LocalDate dueDate
    ) {
        PurchaseInvoice original = purchaseInvoiceRepository.findById(originalInvoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Original purchase invoice not found"));

        if (!original.ledgerId().equals(ledgerId)) {
            throw new IllegalArgumentException("Original purchase invoice does not belong to the given ledger");
        }

        if (original.status() != PurchaseInvoiceStatus.ISSUED && original.status() != PurchaseInvoiceStatus.PAID && original.status() != PurchaseInvoiceStatus.PARTIALLY_PAID) {
            throw new IllegalStateException("Only issued or paid purchase invoices can be credited");
        }

        List<PurchaseInvoiceLine> creditLines = new ArrayList<>();
        for (PurchaseInvoiceLine line : original.lines()) {
            Money negAmount = Money.of(line.amount().amount().negate(), line.amount().currency());
            creditLines.add(new PurchaseInvoiceLine(line.description(), negAmount, line.taxCategory()));
        }

        PurchaseInvoice creditNote = PurchaseInvoice.draft(
                ledgerId,
                creditNoteNumber,
                original.supplierId(),
                issueDate,
                dueDate,
                creditLines
        );
        return purchaseInvoiceRepository.save(creditNote);
    }

    @Override
    public List<PurchaseInvoice> listPurchaseInvoices(LedgerId ledgerId) {
        List<PurchaseInvoice> result = new ArrayList<>();
        for (PurchaseInvoice invoice : purchaseInvoiceRepository.findAll()) {
            if (invoice.ledgerId().equals(ledgerId)) {
                result.add(invoice);
            }
        }
        return result;
    }
}
