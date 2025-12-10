package fr.kovelya.accounting.memory;

import fr.kovelya.accounting.domain.customer.Customer;
import fr.kovelya.accounting.domain.customer.CustomerId;
import fr.kovelya.accounting.domain.repository.CustomerRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryCustomerRepository implements CustomerRepository {

    private final ConcurrentHashMap<String, Customer> byId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Customer> byCode = new ConcurrentHashMap<>();

    @Override
    public Customer save(Customer customer) {
        byId.put(customer.id().value(), customer);
        byCode.put(customer.code(), customer);
        return customer;
    }

    @Override
    public Optional<Customer> findById(CustomerId id) {
        return Optional.ofNullable(byId.get(id.value()));
    }

    @Override
    public Optional<Customer> findByCode(String code) {
        return Optional.ofNullable(byCode.get(code));
    }

    @Override
    public List<Customer> findAll() {
        return new ArrayList<>(byId.values());
    }
}
