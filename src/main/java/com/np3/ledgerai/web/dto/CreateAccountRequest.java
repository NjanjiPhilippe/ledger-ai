package com.np3.ledgerai.web.dto;

import com.np3.ledgerai.domain.valueobject.AccountType;

public record CreateAccountRequest(String name, AccountType type, String currencyCode) {
}
