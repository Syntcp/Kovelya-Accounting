package fr.kovelya.accounting.application;

import fr.kovelya.accounting.domain.invoice.SalesInvoiceId;

public interface InvoicePaymentService {

    void recordPayment(SalesInvoiceId invoiceId, String bankAccountCode);
}
