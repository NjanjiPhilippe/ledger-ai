package com.np3.ledgerai.domain.port;

import java.math.BigDecimal;

public record DebitCreditTotals(BigDecimal totalDebits, BigDecimal totalCredits) {
    public static DebitCreditTotals zero() {
        return new DebitCreditTotals(BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
