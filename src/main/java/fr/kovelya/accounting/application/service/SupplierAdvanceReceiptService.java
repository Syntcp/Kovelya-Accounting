package fr.kovelya.accounting.application.service;

import fr.kovelya.accounting.domain.shared.Money;
import fr.kovelya.accounting.domain.supplier.SupplierId;

import java.time.LocalDate;
import java.util.UUID;

public interface SupplierAdvanceReceiptService {

    void recordUnallocatedPayment(UUID commandId, SupplierId supplierId, String bankAccountCode, Money amount, LocalDate date);

}
