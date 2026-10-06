package com.np3.ledgerai.domain.valueobject;

import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

public record TrialBalance(
        Instant generatedAt,
        Currency currency,
        List<TrialBalanceLine> lines,
        Money totalDebits,
        Money totalCredits
) {
    public TrialBalance {
        Objects.requireNonNull(generatedAt, "generatedAt must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(totalDebits, "totalDebits must not be null");
        Objects.requireNonNull(totalCredits, "totalCredits must not be null");
        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    /**
     * A correctly posted double-entry ledger always nets to zero. This is a
     * structural integrity check on the projection (drift/bug detector), not
     * a business rule the caller has to enforce itself.
     */
    public boolean balanced() {
        return totalDebits.amount().compareTo(totalCredits.amount()) == 0;
    }
}