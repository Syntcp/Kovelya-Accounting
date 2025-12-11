package fr.kovelya.accounting.application.service;

import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceId;

import java.time.LocalDate;

public interface PurchaseInvoicePaymentService {

    void recordPayment(PurchaseInvoiceId invoiceId, String bankAccountCode, LocalDate paymentDate);
}
