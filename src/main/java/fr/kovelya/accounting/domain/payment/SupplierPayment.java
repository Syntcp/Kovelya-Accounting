package fr.kovelya.accounting.domain.payment;

import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceId;
import fr.kovelya.accounting.domain.shared.Money;

import java.time.LocalDate;
import java.util.Objects;

public final class SupplierPayment {

    private final SupplierPaymentId id;
    private final PurchaseInvoiceId invoiceId;
    private final Money amount;
    private final LocalDate paymentDate;
    private final String bankAccountCode;

    public SupplierPayment(SupplierPaymentId id,
                           PurchaseInvoiceId invoiceId,
                           Money amount,
                           LocalDate paymentDate,
                           String bankAccountCode) {
        this.id = Objects.requireNonNull(id);
        this.invoiceId = Objects.requireNonNull(invoiceId);
        this.amount = Objects.requireNonNull(amount);
        this.paymentDate = Objects.requireNonNull(paymentDate);
        this.bankAccountCode = Objects.requireNonNull(bankAccountCode);
    }

    public static SupplierPayment create(PurchaseInvoiceId invoiceId,
                                         Money amount,
                                         LocalDate paymentDate,
                                         String bankAccountCode) {
        return new SupplierPayment(SupplierPaymentId.newId(), invoiceId, amount, paymentDate, bankAccountCode);
    }

    public SupplierPaymentId id() {
        return id;
    }

    public PurchaseInvoiceId invoiceId() {
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