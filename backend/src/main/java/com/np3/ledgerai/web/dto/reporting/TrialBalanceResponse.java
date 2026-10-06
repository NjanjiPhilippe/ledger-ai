package com.np3.ledgerai.web.dto.reporting;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TrialBalanceResponse(
        Instant generatedAt,
        String currencyCode,
        List<TrialBalanceLineResponse> lines,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(type = "string", pattern = "^-?\\d+(\\.\\d+)?$", description = "Exact decimal amount, serialized as a string")
        BigDecimal totalDebits,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(type = "string", pattern = "^-?\\d+(\\.\\d+)?$", description = "Exact decimal amount, serialized as a string")
        BigDecimal totalCredits,
        boolean balanced
) {
}