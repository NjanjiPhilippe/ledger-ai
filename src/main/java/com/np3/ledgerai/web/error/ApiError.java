package com.np3.ledgerai.web.error;

import java.time.Instant;

public record ApiError(int status, String message, Instant timestamp) {
}
