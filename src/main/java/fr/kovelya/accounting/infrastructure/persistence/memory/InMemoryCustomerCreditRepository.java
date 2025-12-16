package fr.kovelya.accounting.infrastructure.persistence.memory;

import fr.kovelya.accounting.domain.credit.CustomerCredit;
import fr.kovelya.accounting.domain.customer.CustomerId;
import fr.kovelya.accounting.domain.repository.CustomerCreditRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.UUID;

public final class InMemoryCustomerCreditRepository implements CustomerCreditRepository {

    private final List<CustomerCredit> storage = new CopyOnWriteArrayList<>();

    @Override
    public CustomerCredit save(CustomerCredit credit) {
        for (int i = 0; i < storage.size(); i++) {
            if (storage.get(i).id().equals(credit.id())) {
                storage.set(i, credit);
                return credit;
            }
        }
        storage.add(credit);
        return credit;
    }

    @Override
    public List<CustomerCredit> findOpenByCustomer(CustomerId customerId) {
        List<CustomerCredit> result = new ArrayList<>();
        for (CustomerCredit c : storage) {
            if (c.customerId().equals(customerId) && c.remaining().amount().compareTo(BigDecimal.ZERO) > 0) {
                result.add(c);
            }
        }
        return result;
    }

    @Override
    public Optional<CustomerCredit> findBySourceCommandId(UUID commandId) {
        for (CustomerCredit c : storage) {
            if (c.sourceCommandId().equals(commandId)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }
}
