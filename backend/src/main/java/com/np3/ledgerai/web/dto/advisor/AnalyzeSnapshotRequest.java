package com.np3.ledgerai.web.dto.advisor;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.List;

public record AnalyzeSnapshotRequest(

        @NotBlank(message = "Currency code is required")
        @Pattern(regexp = "[A-Z]{3}", message = "Currency code must be a 3-letter ISO 4217 code (e.g. XAF, EUR, USD)")
        String currencyCode,

        @NotEmpty(message = "At least one account balance is required")
        @Valid
        List<AccountBalanceLineRequest> balances,

        @NotNull(message = "Total debits is required")
        @Schema(type = "string", pattern = "^-?\\d+(\\.\\d+)?$", description = "Exact decimal amount, as a string (a JSON number is also accepted)")
        BigDecimal totalDebits,

        @NotNull(message = "Total credits is required")
        @Schema(type = "string", pattern = "^-?\\d+(\\.\\d+)?$", description = "Exact decimal amount, as a string (a JSON number is also accepted)")
        BigDecimal totalCredits
) {
}