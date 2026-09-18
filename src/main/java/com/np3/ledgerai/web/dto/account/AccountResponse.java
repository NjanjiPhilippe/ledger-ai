package com.np3.ledgerai.web.dto.account;

import com.np3.ledgerai.domain.valueobject.AccountType;

import java.util.UUID;

public record AccountResponse(UUID id, String name, AccountType type, String currencyCode, boolean active) {
}
