package fr.kovelya.accounting.application.service;

import fr.kovelya.accounting.domain.invoice.SalesInvoiceId;
import fr.kovelya.accounting.domain.shared.Money;

import java.time.LocalDate;

public interface SalesCreditNoteService {

    void issueFullCreditNote(SalesInvoiceId originalInvoiceId, String creditNoteNumber, LocalDate issueDate);

    void issuePartialCreditNote(SalesInvoiceId originalInvoiceId, String creditNoteNumber, LocalDate issueDate, Money grossAmountToCredit);
}
