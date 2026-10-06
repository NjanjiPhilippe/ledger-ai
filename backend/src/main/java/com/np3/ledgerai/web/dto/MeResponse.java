package com.np3.ledgerai.web.dto;

import java.util.UUID;

/**
 * The response that the frontend gets when asking for the current user.
 * Plain identifiers, not the domain value objects, so the JSON is { "userId": "…", "tenantId": "…" }.
 */
public record MeResponse(UUID userId, UUID tenantId) {
}
