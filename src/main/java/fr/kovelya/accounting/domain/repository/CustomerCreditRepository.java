package fr.kovelya.accounting.domain.repository;

import fr.kovelya.accounting.domain.credit.CustomerCredit;
import fr.kovelya.accounting.domain.customer.CustomerId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerCreditRepository {
    CustomerCredit save(CustomerCredit credit);
    List<CustomerCredit> findOpenByCustomer(CustomerId customerId);
    Optional<CustomerCredit> findBySourceCommandId(UUID commandId);
}
