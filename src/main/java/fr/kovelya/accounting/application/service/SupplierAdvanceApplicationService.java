package fr.kovelya.accounting.application.service;

import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceId;
import fr.kovelya.accounting.domain.shared.Money;

import java.time.LocalDate;
import java.util.UUID;

public interface SupplierAdvanceApplicationService {

    void applyAdvance(UUID commandId, PurchaseInvoiceId invoiceId, Money amount, LocalDate date);
}
