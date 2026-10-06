package com.np3.ledgerai.domain.valueobject;

import java.util.Objects;

public record AccountBalanceLine(
        AccountId accountId,
        String accountName,
        AccountType accountType,
        Money balance
) {
    public AccountBalanceLine {
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(accountName, "accountName must not be null");
        Objects.requireNonNull(accountType, "accountType must not be null");
        Objects.requireNonNull(balance, "balance must not be null");
    }
}
