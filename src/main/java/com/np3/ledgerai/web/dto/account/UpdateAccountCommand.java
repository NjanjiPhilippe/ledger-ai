package com.np3.ledgerai.web.dto.account;

import com.np3.ledgerai.domain.valueobject.AccountId;

public record UpdateAccountCommand(AccountId id, String name, boolean active) {
}
