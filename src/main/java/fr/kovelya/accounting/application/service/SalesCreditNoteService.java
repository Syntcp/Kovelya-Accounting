package fr.kovelya.accounting.application.service;

import fr.kovelya.accounting.domain.invoice.SalesInvoiceId;

import java.time.LocalDate;

public interface SalesCreditNoteService {

    void issueFullCreditNote(SalesInvoiceId originalInvoiceId, String creditNoteNumber, LocalDate issueDate);
}
