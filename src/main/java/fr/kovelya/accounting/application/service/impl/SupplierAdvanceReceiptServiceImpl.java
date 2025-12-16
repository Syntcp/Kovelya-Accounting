package fr.kovelya.accounting.application.service.impl;

import fr.kovelya.accounting.application.dto.AccountPosting;
import fr.kovelya.accounting.application.service.AccountingService;
import fr.kovelya.accounting.application.service.SupplierAdvanceReceiptService;
import fr.kovelya.accounting.domain.account.Account;
import fr.kovelya.accounting.domain.advance.SupplierAdvance;
import fr.kovelya.accounting.domain.ledger.JournalType;
import fr.kovelya.accounting.domain.ledger.LedgerEntry;
import fr.kovelya.accounting.domain.repository.AccountRepository;
import fr.kovelya.accounting.domain.repository.SupplierAdvanceRepository;
import fr.kovelya.accounting.domain.shared.Money;
import fr.kovelya.accounting.domain.supplier.SupplierId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class SupplierAdvanceReceiptServiceImpl implements SupplierAdvanceReceiptService {

    private final AccountRepository accountRepository;
    private final AccountingService accountingService;
    private final SupplierAdvanceRepository supplierAdvanceRepository;
    private final String supplierAdvanceAccountCode;
    private final IdempotencyExecutor idempotencyExecutor;

    public SupplierAdvanceReceiptServiceImpl(
            AccountRepository accountRepository,
            AccountingService accountingService,
            SupplierAdvanceRepository supplierAdvanceRepository,
            String supplierAdvanceAccountCode,
            IdempotencyExecutor idempotencyExecutor
    ) {
        this.accountRepository = Objects.requireNonNull(accountRepository);
        this.accountingService = Objects.requireNonNull(accountingService);
        this.supplierAdvanceRepository = Objects.requireNonNull(supplierAdvanceRepository);
        this.supplierAdvanceAccountCode = Objects.requireNonNull(supplierAdvanceAccountCode);
        this.idempotencyExecutor = Objects.requireNonNull(idempotencyExecutor);
    }

    @Override
    public void recordUnallocatedPayment(UUID commandId, SupplierId supplierId, String bankAccountCode, Money amount, LocalDate date) {
        idempotencyExecutor.runVoid(
                commandId,
                () -> doRecord(commandId, supplierId, bankAccountCode, amount, date),
                () -> {}
        );
    }

    private void doRecord(UUID commandId, SupplierId supplierId, String bankAccountCode, Money amount, LocalDate date) {
        if (supplierId == null) throw new IllegalArgumentException("supplierId is required");
        if (bankAccountCode == null || bankAccountCode.isBlank()) throw new IllegalArgumentException("bankAccountCode is required");
        if (amount == null) throw new IllegalArgumentException("amount is required");
        if (date == null) throw new IllegalArgumentException("date is required");
        if (amount.amount().compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("amount must be positive");

        Account bank = accountRepository.findByCode(bankAccountCode)
                .orElseThrow(() -> new IllegalStateException("Bank account not found: " + bankAccountCode));

        Account advance = accountRepository.findByCode(supplierAdvanceAccountCode)
                .orElseThrow(() -> new IllegalStateException("Supplier advances account not found: " + supplierAdvanceAccountCode));

        if (!bank.currency().equals(amount.currency())) throw new IllegalArgumentException("Bank currency mismatch");
        if (!advance.currency().equals(amount.currency())) throw new IllegalArgumentException("Advance currency mismatch");

        String reference = "BANK-ADV-SUP-" + commandId;
        String description = "Unallocated supplier payment (advance)";

        AccountPosting debitAdvance = new AccountPosting(advance.id(), amount, LedgerEntry.Direction.DEBIT);
        AccountPosting creditBank = new AccountPosting(bank.id(), amount, LedgerEntry.Direction.CREDIT);

        accountingService.postJournalTransaction(
                JournalType.BANK,
                reference,
                description,
                date,
                debitAdvance,
                creditBank
        );

        supplierAdvanceRepository.save(SupplierAdvance.create(supplierId, amount, commandId));
    }
}
