package fr.kovelya.accounting.domain.repository;

import fr.kovelya.accounting.domain.customer.Customer;
import fr.kovelya.accounting.domain.customer.CustomerId;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findById(CustomerId id);

    Optional<Customer> findByCode(String code);

    List<Customer> findAll();
}
