package fr.kovelya.accounting.application.service.impl;

import fr.kovelya.accounting.application.service.AccountingService;
import fr.kovelya.accounting.application.service.PurchaseInvoicePaymentService;
import fr.kovelya.accounting.domain.account.Account;
import fr.kovelya.accounting.domain.invoice.SalesInvoice;
import fr.kovelya.accounting.domain.ledger.JournalType;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoice;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceId;
import fr.kovelya.accounting.domain.purchase.PurchaseInvoiceStatus;
import fr.kovelya.accounting.domain.repository.AccountRepository;
import fr.kovelya.accounting.domain.repository.PurchaseInvoiceRepository;

public final class PurchaseInvoicePaymentServiceImpl implements PurchaseInvoicePaymentService {

    private final PurchaseInvoiceRepository purchaseInvoiceRepository;
    private final AccountRepository accountRepository;
    private final AccountingService accountingService;
    private final String payableAccountCode;

    public PurchaseInvoicePaymentServiceImpl(PurchaseInvoiceRepository purchaseInvoiceRepository, AccountRepository accountRepository, AccountingService accountingService, String payableAccountCode) {
        this.purchaseInvoiceRepository = purchaseInvoiceRepository;
        this.accountRepository = accountRepository;
        this.accountingService = accountingService;
        this.payableAccountCode = payableAccountCode;
    }

    @Override
    public void recordPayment(PurchaseInvoiceId invoiceId, String bankAccountCode) {
        PurchaseInvoice invoice = purchaseInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase invoice not found"));

        if (invoice.status() == PurchaseInvoiceStatus.PAID) {
            throw new IllegalStateException("Purchase invoice is already paid");
        }

        if (invoice.status() != PurchaseInvoiceStatus.ISSUED) {
            throw new IllegalStateException("Only issued purchases invoices can be paid");
        }

        Account payable = accountRepository.findByCode(payableAccountCode)
                .orElseThrow(() -> new IllegalStateException("Payable account not found: " + payableAccountCode));

        Account bank = accountRepository.findByCode(bankAccountCode)
                .orElseThrow(() -> new IllegalStateException("Bank account not found: " + bankAccountCode));

        accountingService.transfer(
                payable.id(),
                bank.id(),
                invoice.total(),
                JournalType.BANK,
                "Payment of purchase invoice " + invoice.number()
        );

        PurchaseInvoice paid = invoice.markPaid();
        purchaseInvoiceRepository.save(paid);
    }
}
