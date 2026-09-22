package com.np3.ledgerai.infrastructure.advisor.common;

public record RecommendationJson(
        String title,
        String detail,
        String category,
        String severity
) {
}