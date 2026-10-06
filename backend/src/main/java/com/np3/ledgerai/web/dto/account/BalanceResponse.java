package com.np3.ledgerai.web.dto.account;

import java.math.BigDecimal;

public record BalanceResponse(BigDecimal amount, String currencyCode) {
}
