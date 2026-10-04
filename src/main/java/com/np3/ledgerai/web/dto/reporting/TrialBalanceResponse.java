package com.np3.ledgerai.web.dto.reporting;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TrialBalanceResponse(
        Instant generatedAt,
        String currencyCode,
        List<TrialBalanceLineResponse> lines,
        BigDecimal totalDebits,
        BigDecimal totalCredits,
        boolean balanced
) {
}