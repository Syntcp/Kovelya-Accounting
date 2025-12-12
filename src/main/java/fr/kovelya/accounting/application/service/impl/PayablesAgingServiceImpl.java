package fr.kovelya.accounting.application.service.impl;

import fr.kovelya.accounting.application.report.SupplierPayableAgingView;
import fr.kovelya.accounting.application.service.PayablesAgingService;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoice;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceStatus;
import fr.kovelya.accounting.domain.payment.SupplierPayment;
import fr.kovelya.accounting.domain.repository.PurchaseInvoiceRepository;
import fr.kovelya.accounting.domain.repository.SupplierPaymentRepository;
import fr.kovelya.accounting.domain.repository.SupplierRepository;
import fr.kovelya.accounting.domain.shared.Money;
import fr.kovelya.accounting.domain.supplier.Supplier;
import fr.kovelya.accounting.domain.supplier.SupplierId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public final class PayablesAgingServiceImpl implements PayablesAgingService {

    private final SupplierRepository supplierRepository;
    private final PurchaseInvoiceRepository purchaseInvoiceRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;

    public PayablesAgingServiceImpl(SupplierRepository supplierRepository, PurchaseInvoiceRepository purchaseInvoiceRepository, SupplierPaymentRepository supplierPaymentRepository) {
        this.supplierRepository = supplierRepository;
        this.purchaseInvoiceRepository = purchaseInvoiceRepository;
        this.supplierPaymentRepository = supplierPaymentRepository;
    }

    @Override
    public List<SupplierPayableAgingView> getSupplierAging(LocalDate asOfDate) {
        List<SupplierPayableAgingView> result = new ArrayList<>();

        List<Supplier> suppliers = supplierRepository.findAll();
        for (Supplier supplier : suppliers) {
            SupplierPayableAgingView view = computeForSupplier(supplier, asOfDate);
            if (view != null && view.total().amount().compareTo(BigDecimal.ZERO) > 0) {
                result.add(view);
            }
        }

        return result;
    }

    private SupplierPayableAgingView computeForSupplier(Supplier supplier, LocalDate asOfDate) {
        SupplierId supplierId = supplier.id();
        List<PurchaseInvoice> invoices = purchaseInvoiceRepository.findBySupplier(supplierId);

        Money notDue = null;
        Money due0_30 = null;
        Money due31_60 = null;
        Money due61_90 = null;
        Money due90Plus = null;

        for (PurchaseInvoice invoice : invoices) {
            if (invoice.status() == PurchaseInvoiceStatus.PAID
                    || invoice.status() == PurchaseInvoiceStatus.CANCELLED) {
                continue;
            }

            Money total = invoice.total();
            Money paid = Money.zero(total.currency());

            List<SupplierPayment> payments = supplierPaymentRepository.findByInvoice(invoice.id());
            for (SupplierPayment payment : payments) {
                paid = paid.add(payment.amount());
            }

            Money amount = total.subtract(paid);
            if (amount.amount().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            if (notDue == null) {
                notDue = Money.zero(amount.currency());
                due0_30 = Money.zero(amount.currency());
                due31_60 = Money.zero(amount.currency());
                due61_90 = Money.zero(amount.currency());
                due90Plus = Money.zero(amount.currency());
            }

            long daysPastDue = ChronoUnit.DAYS.between(invoice.dueDate(), asOfDate);

            if (daysPastDue < 0) {
                notDue = notDue.add(amount);
            } else if (daysPastDue <= 30) {
                due0_30 = due0_30.add(amount);
            } else if (daysPastDue <= 60) {
                due31_60 = due31_60.add(amount);
            } else if (daysPastDue <= 90) {
                due61_90 = due61_90.add(amount);
            } else {
                due90Plus = due90Plus.add(amount);
            }
        }

        if (notDue == null) {
            return null;
        }

        Money total = notDue
                .add(due0_30)
                .add(due31_60)
                .add(due61_90)
                .add(due90Plus);

        if (total.amount().compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return new SupplierPayableAgingView(
                supplier,
                notDue,
                due0_30,
                due31_60,
                due61_90,
                due90Plus,
                total
        );
    }
}
