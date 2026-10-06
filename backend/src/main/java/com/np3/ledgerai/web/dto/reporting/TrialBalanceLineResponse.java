package com.np3.ledgerai.web.dto.reporting;

import java.math.BigDecimal;
import java.util.UUID;

public record TrialBalanceLineResponse(
        UUID accountId,
        String accountName,
        String accountType,
        BigDecimal totalDebits,
        BigDecimal totalCredits,
        BigDecimal balance
) {
}