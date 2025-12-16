package fr.kovelya.accounting.application.service;

import fr.kovelya.accounting.domain.invoice.SalesInvoiceId;
import fr.kovelya.accounting.domain.shared.Money;

import java.time.LocalDate;
import java.util.UUID;

public interface CustomerCreditApplicationService {
    void applyCredit(UUID commandId, SalesInvoiceId invoiceId, Money amount, LocalDate date);
}
