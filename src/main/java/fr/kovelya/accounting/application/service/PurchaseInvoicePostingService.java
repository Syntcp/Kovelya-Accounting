package fr.kovelya.accounting.application.service;

import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceId;

public interface PurchaseInvoicePostingService {

    void postPurchaseInvoice(PurchaseInvoiceId invoiceId);
}
