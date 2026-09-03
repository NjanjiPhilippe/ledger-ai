package com.np3.ledgerai.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {
    public UserId {
        Objects.requireNonNull(value, "UserId value must not be null");
    }

    public static UserId of(String rawSubjectId) {
        Objects.requireNonNull(rawSubjectId, "rawSubjectId must not be null");
        try {
            return new UserId(UUID.fromString(rawSubjectId));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Keycloak subject id is not a valid UUID: " + rawSubjectId, e);
        }
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }

    public String asString() {
        return value.toString();
    }
}
