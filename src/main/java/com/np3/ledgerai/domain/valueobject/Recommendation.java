package com.np3.ledgerai.domain.valueobject;

import java.util.Objects;

public record Recommendation(
        String title,
        String detail,
        AdviceCategory category,
        AdviceSeverity severity
) {
    public Recommendation {
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(detail, "detail must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
    }
}
