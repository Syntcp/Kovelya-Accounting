package fr.kovelya.accounting.application.service;

import fr.kovelya.accounting.application.dto.PurchaseInvoicePaymentAllocation;

import java.time.LocalDate;
import java.util.UUID;

public interface SupplierBatchPaymentService {

    void recordBatchPayment(UUID commandId, String bankAccountCode, LocalDate paymentDate, PurchaseInvoicePaymentAllocation... allocations);
}
