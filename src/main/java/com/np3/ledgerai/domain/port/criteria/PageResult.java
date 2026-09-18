package com.np3.ledgerai.domain.port.criteria;

import java.util.List;

public record PageResult<T>(List<T> content, int page, int size, long totalElements) {
    public int totalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }
}
