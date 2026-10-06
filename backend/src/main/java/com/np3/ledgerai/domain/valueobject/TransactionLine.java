package com.np3.ledgerai.domain.valueobject;

import java.util.Objects;

public record TransactionLine(AccountId accountId, Money amount, EntryType entryType) {
    public TransactionLine {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(entryType, "entryType");
        if (amount.isNegative() || amount.amount().signum() == 0) {
            throw new IllegalArgumentException("TransactionLine amount must be strictly positive");
        }
    }

    public boolean isDebit() {
        return entryType == EntryType.DEBIT;
    }

    public boolean isCredit() {
        return entryType == EntryType.CREDIT;
    }
}