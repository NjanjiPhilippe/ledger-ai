package com.np3.ledgerai.domain.port.criteria;

public record PageRequest(int page, int size) {
    public PageRequest {
        if (page < 0) throw new IllegalArgumentException("page must not be negative");
        if (size < 1 || size > 200) throw new IllegalArgumentException("size must be between 1 and 200");
    }
}
