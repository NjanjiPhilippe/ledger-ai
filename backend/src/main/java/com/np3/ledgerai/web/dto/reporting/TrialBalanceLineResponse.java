package com.np3.ledgerai.web.dto.reporting;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

public record TrialBalanceLineResponse(
        UUID accountId,
        String accountName,
        String accountType,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(type = "string", pattern = "^-?\\d+(\\.\\d+)?$", description = "Exact decimal amount, serialized as a string")
        BigDecimal totalDebits,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(type = "string", pattern = "^-?\\d+(\\.\\d+)?$", description = "Exact decimal amount, serialized as a string")
        BigDecimal totalCredits,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(type = "string", pattern = "^-?\\d+(\\.\\d+)?$", description = "Exact decimal amount, serialized as a string")
        BigDecimal balance
) {
}