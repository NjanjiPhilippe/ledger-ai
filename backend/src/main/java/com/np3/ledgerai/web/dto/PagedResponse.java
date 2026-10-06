package com.np3.ledgerai.web.dto;

import com.np3.ledgerai.domain.port.criteria.PageResult;

import java.util.List;
import java.util.function.Function;

public record PagedResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <D, T> PagedResponse<D> from(PageResult<T> result, Function<T, D> mapper) {
        return new PagedResponse<>(
                result.content().stream().map(mapper).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages());
    }
}
