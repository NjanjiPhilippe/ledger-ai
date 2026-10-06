package com.np3.ledgerai.web.dto.account;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record BalanceResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(type = "string", pattern = "^-?\\d+(\\.\\d+)?$", description = "Exact decimal amount, serialized as a string")
        BigDecimal amount,
        String currencyCode) {
}
