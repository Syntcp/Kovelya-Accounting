package fr.kovelya.accounting.application.dto;

import fr.kovelya.accounting.domain.invoice.SalesInvoiceId;
import fr.kovelya.accounting.domain.shared.Money;

public record SalesInvoicePaymentAllocation(SalesInvoiceId invoiceId, Money amount) {
}
