package fr.kovelya.accounting.application;

import fr.kovelya.accounting.domain.customer.Customer;
import fr.kovelya.accounting.domain.shared.Money;

public final class CustomerReceivableAgingView {

    private final Customer customer;
    private final Money notDue;
    private final Money due0_30;
    private final Money due31_60;
    private final Money due61_90;
    private final Money due90Plus;
    private final Money total;

    public CustomerReceivableAgingView(Customer customer, Money notDue, Money due0_30, Money due31_60, Money due61_90, Money due90Plus, Money total) {
        this.customer = customer;
        this.notDue = notDue;
        this.due0_30 = due0_30;
        this.due31_60 = due31_60;
        this.due61_90 = due61_90;
        this.due90Plus = due90Plus;
        this.total = total;
    }

    public Customer customer() {
        return customer;
    }

    public Money notDue() {
        return notDue;
    }

    public Money due0_30() {
        return due0_30;
    }

    public Money due31_60() {
        return due31_60;
    }

    public Money due61_90() {
        return due61_90;
    }

    public Money due90Plus() {
        return due90Plus;
    }

    public Money total() {
        return total;
    }
}
