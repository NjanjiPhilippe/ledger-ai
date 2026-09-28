package com.np3.ledgerai.web.dto.advisor;

public record RecommendationResponse(
        String title,
        String detail,
        String category,
        String severity
) {
}