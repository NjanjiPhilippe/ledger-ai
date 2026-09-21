package com.np3.ledgerai.web.dto.reporting;

import com.np3.ledgerai.domain.valueobject.AccountType;

import java.math.BigDecimal;
import java.util.UUID;

public record TrialBalanceLineResponse(UUID accountId, String accountName, AccountType accountType,
                                       BigDecimal balance) {
}
