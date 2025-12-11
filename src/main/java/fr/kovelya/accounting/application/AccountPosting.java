package fr.kovelya.accounting.application;

import fr.kovelya.accounting.domain.account.AccountId;
import fr.kovelya.accounting.domain.ledger.LedgerEntry;
import fr.kovelya.accounting.domain.shared.Money;

public final class AccountPosting {

    private final AccountId accountId;
    private final Money amount;
    private final LedgerEntry.Direction direction;

    public AccountPosting(AccountId accountId, Money amount, LedgerEntry.Direction direction) {
        this.accountId = accountId;
        this.amount = amount;
        this.direction = direction;
    }

    public AccountId accountId() {
        return accountId;
    }

    public Money amount() {
        return amount;
    }

    public LedgerEntry.Direction direction() {
        return direction;
    }
}
