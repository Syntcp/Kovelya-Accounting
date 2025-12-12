package fr.kovelya.accounting.application.service.impl;

import fr.kovelya.accounting.application.dto.AccountPosting;
import fr.kovelya.accounting.application.service.AccountingService;
import fr.kovelya.accounting.application.service.PeriodClosingService;
import fr.kovelya.accounting.domain.account.Account;
import fr.kovelya.accounting.domain.account.AccountId;
import fr.kovelya.accounting.domain.account.AccountType;
import fr.kovelya.accounting.domain.ledger.JournalType;
import fr.kovelya.accounting.domain.ledger.LedgerEntry;
import fr.kovelya.accounting.domain.period.AccountingPeriod;
import fr.kovelya.accounting.domain.period.PeriodStatus;
import fr.kovelya.accounting.domain.repository.AccountRepository;
import fr.kovelya.accounting.domain.repository.AccountingPeriodRepository;
import fr.kovelya.accounting.domain.shared.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class PeriodClosingServiceImpl implements PeriodClosingService {

    private final AccountingService accountingService;
    private final AccountingPeriodRepository accountingPeriodRepository;
    private final AccountRepository accountRepository;

    public PeriodClosingServiceImpl(AccountingService accountingService, AccountingPeriodRepository accountingPeriodRepository, AccountRepository accountRepository) {
        this.accountingService = accountingService;
        this.accountingPeriodRepository = accountingPeriodRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    public void closePeriod(AccountingPeriod period, AccountId retainedEarningsAccountId) {
        if (period.status() != PeriodStatus.OPEN) {
            throw new IllegalStateException("Only open periods can be closed");
        }

        Account retainedEarnings = accountRepository.findById(retainedEarningsAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Retained earnings account not found"));

        if (retainedEarnings.type() != AccountType.EQUITY) {
            throw new IllegalArgumentException("Retained earnings account must be of type EQUITY");
        }

        List<AccountPosting> postings = new ArrayList<>();

        for (Account account : accountingService.listAccounts(period.ledgerId())) {
            if (account.type() != AccountType.INCOME && account.type() != AccountType.EXPENSE) {
                continue;
            }

            Money balance = accountingService.getBalanceForPeriod(account.id(), period);
            BigDecimal amount = balance.amount();
            if (amount.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            BigDecimal absAmount = amount.abs();
            Money closingAmount = Money.of(absAmount, balance.currency());

            LedgerEntry.Direction accountDirection;
            LedgerEntry.Direction equityDirection;

            if (account.type() == AccountType.INCOME) {
                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    accountDirection = LedgerEntry.Direction.DEBIT;
                    equityDirection = LedgerEntry.Direction.CREDIT;
                } else {
                    accountDirection = LedgerEntry.Direction.CREDIT;
                    equityDirection = LedgerEntry.Direction.DEBIT;
                }
            } else {
                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    accountDirection = LedgerEntry.Direction.CREDIT;
                    equityDirection = LedgerEntry.Direction.DEBIT;
                } else {
                    accountDirection = LedgerEntry.Direction.DEBIT;
                    equityDirection = LedgerEntry.Direction.CREDIT;
                }
            }

            postings.add(new AccountPosting(account.id(), closingAmount, accountDirection));
            postings.add(new AccountPosting(retainedEarnings.id(), closingAmount, equityDirection));
        }

        if (!postings.isEmpty()) {
            LocalDate closingDate = period.endDate();
            String reference = "CLOSE-" + period.name();
            String description = "Closing entries for period " + period.name();

            accountingService.postJournalTransaction(
                    JournalType.ADJUSTMENT,
                    reference,
                    description,
                    closingDate,
                    postings.toArray(new AccountPosting[0])
            );
        }

        AccountingPeriod closed = period.close();
        accountingPeriodRepository.save(closed);
    }
}
