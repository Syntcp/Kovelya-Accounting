package fr.kovelya.accounting.application.service;

import fr.kovelya.accounting.domain.invoice.SalesInvoiceId;

import java.time.LocalDate;

public interface InvoicePaymentService {

    void recordPayment(SalesInvoiceId invoiceId, String bankAccountCode, LocalDate paymentDate);
}
