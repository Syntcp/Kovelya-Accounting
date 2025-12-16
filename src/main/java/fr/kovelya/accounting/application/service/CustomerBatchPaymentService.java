package fr.kovelya.accounting.application.service;

import fr.kovelya.accounting.application.dto.SalesInvoicePaymentAllocation;

import java.time.LocalDate;
import java.util.UUID;

public interface CustomerBatchPaymentService {

    void recordBatchPayment(UUID commandId, String bankAccountCode, LocalDate paymentDate, SalesInvoicePaymentAllocation... allocations);
}
