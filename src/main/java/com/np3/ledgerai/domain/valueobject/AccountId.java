package com.np3.ledgerai.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record AccountId(UUID value) {
    public AccountId {
        Objects.requireNonNull(value, "value");
    }
    public static AccountId of(UUID value) {
        return new AccountId(value);
    }

    public static AccountId generate() {
        return new AccountId(UUID.randomUUID());
    }
}
