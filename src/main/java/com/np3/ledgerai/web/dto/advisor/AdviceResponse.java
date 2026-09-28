package com.np3.ledgerai.web.dto.advisor;

import java.time.Instant;
import java.util.List;

public record AdviceResponse(
        Instant generatedAt,
        String provider,
        List<RecommendationResponse> recommendations
) {
}