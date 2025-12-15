package fr.kovelya.accounting.application.service;

import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceId;
import fr.kovelya.accounting.domain.shared.Money;

import java.time.LocalDate;
import java.util.UUID;

public interface PurchaseInvoicePaymentService {

    void recordPayment(UUID commandId, PurchaseInvoiceId invoiceId, String bankAccountCode, Money amount, LocalDate paymentDate);

    default void recordPayment(PurchaseInvoiceId invoiceId, String bankAccountCode, LocalDate paymentDate) {
        recordPayment(UUID.randomUUID(), invoiceId, bankAccountCode, null, paymentDate);
    }
}
