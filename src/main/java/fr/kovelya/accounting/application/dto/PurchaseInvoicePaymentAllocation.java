package fr.kovelya.accounting.application.dto;

import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceId;
import fr.kovelya.accounting.domain.shared.Money;

public record PurchaseInvoicePaymentAllocation(PurchaseInvoiceId invoiceId, Money amount) {
}
