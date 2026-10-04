package com.np3.ledgerai.domain.valueobject;

import java.util.Objects;

public record TrialBalanceLine(
        AccountId accountId,
        String accountName,
        AccountType accountType,
        Money totalDebits,
        Money totalCredits,
        Money balance
) {
    public TrialBalanceLine {
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(accountName, "accountName must not be null");
        Objects.requireNonNull(accountType, "accountType must not be null");
        Objects.requireNonNull(totalDebits, "totalDebits must not be null");
        Objects.requireNonNull(totalCredits, "totalCredits must not be null");
        Objects.requireNonNull(balance, "balance must not be null");
    }
}