package com.np3.ledgerai.domain.exception;

import com.np3.ledgerai.domain.valueobject.AccountId;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(AccountId id) {
        super("Account not found: " + id.value());
    }
}
