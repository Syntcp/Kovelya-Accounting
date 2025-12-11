package fr.kovelya.accounting.application;

import fr.kovelya.accounting.domain.invoice.SalesInvoiceId;

public interface InvoicePostingService {
    void postInvoice(SalesInvoiceId invoiceId);
}
