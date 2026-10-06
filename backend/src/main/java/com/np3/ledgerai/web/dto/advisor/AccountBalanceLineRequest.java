package com.np3.ledgerai.web.dto.advisor;

import com.np3.ledgerai.domain.valueobject.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountBalanceLineRequest(

        @NotNull(message = "Account id is required")
        UUID accountId,

        @NotBlank(message = "Account name is required")
        String accountName,

        @NotNull(message = "Account type is required")
        AccountType accountType,

        @NotNull(message = "Balance is required")
        @Schema(type = "string", pattern = "^-?\\d+(\\.\\d+)?$", description = "Exact decimal amount, as a string (a JSON number is also accepted)")
        BigDecimal balance
) {
}
