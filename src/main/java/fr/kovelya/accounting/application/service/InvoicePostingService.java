package fr.kovelya.accounting.application.service;

import fr.kovelya.accounting.domain.invoice.SalesInvoiceId;

public interface InvoicePostingService {
    void postInvoice(SalesInvoiceId invoiceId);
}
