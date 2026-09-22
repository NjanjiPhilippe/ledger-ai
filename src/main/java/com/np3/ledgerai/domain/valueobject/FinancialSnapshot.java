package com.np3.ledgerai.domain.valueobject;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record FinancialSnapshot(
        Instant generatedAt,
        String currencyCode,
        List<AccountBalanceLine> balances,
        Money totalDebits,
        Money totalCredits
) {
    public FinancialSnapshot {
        Objects.requireNonNull(generatedAt, "generatedAt must not be null");
        Objects.requireNonNull(currencyCode, "currencyCode must not be null");
        Objects.requireNonNull(totalDebits, "totalDebits must not be null");
        Objects.requireNonNull(totalCredits, "totalCredits must not be null");
        balances = balances == null ? List.of() : List.copyOf(balances);
    }
}