package com.np3.ledgerai.web.error;

import java.time.Instant;
import java.util.List;

public record ValidationApiError(int status, String message, Instant timestamp, List<FieldError> errors) {

    public record FieldError(String field, String message) {
    }
}
