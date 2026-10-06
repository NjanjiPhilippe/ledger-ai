package com.np3.ledgerai.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record TenantId(UUID value) {
    public TenantId {
        Objects.requireNonNull(value, "value");
    }
    public static TenantId of(UUID value) {
        return new TenantId(value);
    }
}
