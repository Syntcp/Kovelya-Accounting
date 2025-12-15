package fr.kovelya.accounting.application.service;

import fr.kovelya.accounting.domain.invoice.SalesInvoiceId;
import fr.kovelya.accounting.domain.shared.Money;

import java.time.LocalDate;
import java.util.UUID;

public interface InvoicePaymentService {

    void recordPayment(UUID commandId, SalesInvoiceId invoiceId, String bankAccountCode, Money amount, LocalDate paymentDate);

    default void recordPayment(SalesInvoiceId invoiceId, String bankAccountCode, LocalDate paymentDate) {
        recordPayment(UUID.randomUUID(), invoiceId, bankAccountCode, null, paymentDate);
    }
}
