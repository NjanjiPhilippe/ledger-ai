package com.np3.ledgerai.domain.valueobject;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record AdviceResult(
        Instant generatedAt,
        String provider,
        List<Recommendation> recommendations
) {
    public AdviceResult {
        Objects.requireNonNull(generatedAt, "generatedAt must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
    }
}