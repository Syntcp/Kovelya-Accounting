package fr.kovelya.accounting.domain.payment;

import fr.kovelya.accounting.domain.invoice.SalesInvoiceId;
import fr.kovelya.accounting.domain.shared.Money;

import java.time.LocalDate;
import java.util.Objects;

public final class CustomerPayment {

    private final CustomerPaymentId id;
    private final SalesInvoiceId invoiceId;
    private final Money amount;
    private final LocalDate paymentDate;
    private final String bankAccountCode;

    public CustomerPayment(CustomerPaymentId id, SalesInvoiceId invoiceId, Money amount, LocalDate paymentDate, String bankAccountCode) {
        this.id = Objects.requireNonNull(id);
        this.invoiceId = Objects.requireNonNull(invoiceId);
        this.amount = Objects.requireNonNull(amount);
        this.paymentDate = Objects.requireNonNull(paymentDate);
        this.bankAccountCode = Objects.requireNonNull(bankAccountCode);
    }

    public static CustomerPayment create(SalesInvoiceId invoiceId, Money amount, LocalDate paymentDate, String bankAccountCode) {
        return new CustomerPayment(CustomerPaymentId.newId(), invoiceId, amount, paymentDate, bankAccountCode);
    }

    public CustomerPaymentId id() {
        return id;
    }

    public SalesInvoiceId invoiceId() {
        return invoiceId;
    }

    public Money amount() {
        return amount;
    }

    public LocalDate paymentDate() {
        return paymentDate;
    }

    public String bankAccountCode() {
        return bankAccountCode;
    }
}
