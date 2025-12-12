package fr.kovelya.accounting.application.service;

import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceId;
import fr.kovelya.accounting.domain.shared.Money;

import java.time.LocalDate;

public interface PurchaseInvoicePaymentService {

    void recordPayment(PurchaseInvoiceId invoiceId, String bankAccountCode, Money amount, LocalDate paymentDate);

    default void recordPayment(PurchaseInvoiceId invoiceId, String bankAccountCode, LocalDate paymentDate) {
        recordPayment(invoiceId, bankAccountCode, null, paymentDate);
    }
}
