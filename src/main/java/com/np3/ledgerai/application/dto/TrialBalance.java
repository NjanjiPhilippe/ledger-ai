package com.np3.ledgerai.application.dto;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

public record TrialBalance(List<TrialBalanceLine> lines, BigDecimal totalDebits, BigDecimal totalCredits,
                           Currency currency, boolean balanced) {
}
