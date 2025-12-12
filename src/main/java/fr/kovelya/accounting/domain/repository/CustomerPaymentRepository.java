package fr.kovelya.accounting.domain.repository;

import fr.kovelya.accounting.domain.invoice.SalesInvoiceId;
import fr.kovelya.accounting.domain.payment.CustomerPayment;
import fr.kovelya.accounting.domain.payment.CustomerPaymentId;

import java.util.List;
import java.util.Optional;

public interface CustomerPaymentRepository {

    CustomerPayment save(CustomerPayment payment);

    List<CustomerPayment> findByInvoice(SalesInvoiceId invoiceId);

    List<CustomerPayment> findAll();

    Optional<CustomerPayment> findById(CustomerPaymentId id);
}
