package com.np3.ledgerai.domain.port;

import java.math.BigDecimal;

public record DebitCreditTotals(BigDecimal totalDebits, BigDecimal totalCredits) {
    /** Same amounts, ignoring scale (100 and 100.0000 are the same total). */
    public boolean sameAmountsAs(DebitCreditTotals other) {
        return totalDebits.compareTo(other.totalDebits) == 0 && totalCredits.compareTo(other.totalCredits) == 0;
    }

    public static DebitCreditTotals zero() {
        return new DebitCreditTotals(BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
