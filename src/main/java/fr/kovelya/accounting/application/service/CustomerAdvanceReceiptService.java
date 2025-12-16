package fr.kovelya.accounting.application.service;

import fr.kovelya.accounting.domain.customer.CustomerId;
import fr.kovelya.accounting.domain.shared.Money;

import java.time.LocalDate;
import java.util.UUID;

public interface CustomerAdvanceReceiptService {

    void recordUnallocatedPayment(UUID commandId, CustomerId customerId, String bankAccountCode, Money amount, LocalDate date);
}
