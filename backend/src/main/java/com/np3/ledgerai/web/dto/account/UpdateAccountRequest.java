package com.np3.ledgerai.web.dto.account;

import jakarta.validation.constraints.NotBlank;

public record UpdateAccountRequest(
        @NotBlank(message = "Name is required")
        String name, boolean active) {
}
