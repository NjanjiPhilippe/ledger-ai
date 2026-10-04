package com.np3.ledgerai.domain.model;

import com.np3.ledgerai.domain.valueobject.UserId;

import java.time.Instant;
import java.util.Objects;

/**
 * Application-specific data attached to an authenticated user.
 * Keycloak stays the source of truth for identity; this only holds business data.
 */
public record UserProfile(UserId userId, String displayName, String companyId, String roleInCompany,
                          Instant createdAt) {
    public UserProfile {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    /** A profile with no business data yet, to be completed later by a dedicated use case. */
    public static UserProfile provision(UserId userId, Instant createdAt) {
        return new UserProfile(userId, null, null, null, createdAt);
    }
}
