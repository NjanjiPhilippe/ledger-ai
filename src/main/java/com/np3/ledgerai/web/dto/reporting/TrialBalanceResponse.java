package com.np3.ledgerai.web.dto.reporting;

import java.math.BigDecimal;
import java.util.List;

public record TrialBalanceResponse(List<TrialBalanceLineResponse> lines, BigDecimal totalDebits,
                                   BigDecimal totalCredits, String currencyCode, boolean balanced) {
}
