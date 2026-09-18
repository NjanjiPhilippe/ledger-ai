package com.np3.ledgerai.application.account.command;

import com.np3.ledgerai.domain.valueobject.AccountId;

public record UpdateAccountCommand(AccountId id, String name, boolean active) {
}
