package fr.kovelya.accounting.domain.credit;

import fr.kovelya.accounting.domain.customer.CustomerId;
import fr.kovelya.accounting.domain.shared.Money;

import java.util.UUID;

public record CustomerCredit(CustomerCreditId id, CustomerId customerId, Money remaining, UUID sourceCommandId) {

    public static CustomerCredit create(CustomerId customerId, Money remaining, UUID sourceCommandId) {
        return new CustomerCredit(new CustomerCreditId(UUID.randomUUID()), customerId, remaining, sourceCommandId);
    }

    public CustomerCredit consume(Money amount) {
        return new CustomerCredit(id, customerId, remaining.subtract(amount), sourceCommandId);
    }
}
