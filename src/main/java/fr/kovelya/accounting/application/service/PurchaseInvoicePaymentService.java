package fr.kovelya.accounting.application.service;

import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceId;

public interface PurchaseInvoicePaymentService {

    void recordPayment(PurchaseInvoiceId invoiceId, String bankAccountCode);
}
