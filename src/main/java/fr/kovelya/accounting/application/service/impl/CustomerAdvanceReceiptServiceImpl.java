package fr.kovelya.accounting.application.service.impl;

import fr.kovelya.accounting.application.dto.AccountPosting;
import fr.kovelya.accounting.application.service.AccountingService;
import fr.kovelya.accounting.application.service.CustomerAdvanceReceiptService;
import fr.kovelya.accounting.domain.account.Account;
import fr.kovelya.accounting.domain.customer.CustomerId;
import fr.kovelya.accounting.domain.ledger.JournalType;
import fr.kovelya.accounting.domain.ledger.LedgerEntry;
import fr.kovelya.accounting.domain.repository.AccountRepository;
import fr.kovelya.accounting.domain.repository.CustomerCreditRepository;
import fr.kovelya.accounting.domain.shared.Money;
import fr.kovelya.accounting.domain.credit.CustomerCredit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class CustomerAdvanceReceiptServiceImpl implements CustomerAdvanceReceiptService {

    private final AccountRepository accountRepository;
    private final AccountingService accountingService;
    private final CustomerCreditRepository customerCreditRepository;
    private final String customerAdvanceAccountCode;
    private final IdempotencyExecutor idempotencyExecutor;

    public CustomerAdvanceReceiptServiceImpl(
            AccountRepository accountRepository,
            AccountingService accountingService,
            CustomerCreditRepository customerCreditRepository,
            String customerAdvanceAccountCode,
            IdempotencyExecutor idempotencyExecutor
    ) {
        this.accountRepository = Objects.requireNonNull(accountRepository);
        this.accountingService = Objects.requireNonNull(accountingService);
        this.customerCreditRepository = Objects.requireNonNull(customerCreditRepository);
        this.customerAdvanceAccountCode = Objects.requireNonNull(customerAdvanceAccountCode);
        this.idempotencyExecutor = Objects.requireNonNull(idempotencyExecutor);
    }

    @Override
    public void recordUnallocatedPayment(UUID commandId, CustomerId customerId, String bankAccountCode, Money amount, LocalDate date) {
        idempotencyExecutor.runVoid(
                commandId,
                () -> doRecord(commandId, customerId, bankAccountCode, amount, date),
                () -> {}
        );
    }

    private void doRecord(UUID commandId, CustomerId customerId, String bankAccountCode, Money amount, LocalDate date) {
        if (customerId == null) throw new IllegalArgumentException("customerId is required");
        if (bankAccountCode == null || bankAccountCode.isBlank()) throw new IllegalArgumentException("bankAccountCode is required");
        if (amount == null) throw new IllegalArgumentException("amount is required");
        if (date == null) throw new IllegalArgumentException("date is required");
        if (amount.amount().compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("amount must be positive");

        Account bank = accountRepository.findByCode(bankAccountCode)
                .orElseThrow(() -> new IllegalStateException("Bank account not found: " + bankAccountCode));

        Account advance = accountRepository.findByCode(customerAdvanceAccountCode)
                .orElseThrow(() -> new IllegalStateException("Customer advances account not found: " + customerAdvanceAccountCode));

        if (!bank.currency().equals(amount.currency())) throw new IllegalArgumentException("Bank currency mismatch");
        if (!advance.currency().equals(amount.currency())) throw new IllegalArgumentException("Advance currency mismatch");

        String reference = "BANK-ADV-CUST-" + commandId;
        String description = "Unallocated customer payment (advance)";

        AccountPosting debitBank = new AccountPosting(bank.id(), amount, LedgerEntry.Direction.DEBIT);
        AccountPosting creditAdvance = new AccountPosting(advance.id(), amount, LedgerEntry.Direction.CREDIT);

        accountingService.postJournalTransaction(
                JournalType.BANK,
                reference,
                description,
                date,
                debitBank,
                creditAdvance
        );

        customerCreditRepository.save(CustomerCredit.create(customerId, amount, commandId));
    }
}
