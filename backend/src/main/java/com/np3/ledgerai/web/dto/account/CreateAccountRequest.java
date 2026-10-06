package com.np3.ledgerai.web.dto.account;

import com.np3.ledgerai.domain.valueobject.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateAccountRequest(@NotBlank(message = "Name is required")
                                          String name,

                                          @NotNull(message = "Account type is required")
                                          AccountType type,

                                          @NotBlank(message = "Currency code is required")
                                          @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be a 3-letter ISO code (e.g. XAF, USD)")
                                          String currencyCode) {
}
