package com.platform.backend.shared.domain.pagination;

import java.util.List;
import java.util.function.Function;

/** One page of a list plus how many records the whole list has, so clients can draw the page buttons. */
public record PageResult<T>(List<T> content, long totalElements, PageQuery query) {

    public static <T> PageResult<T> empty(PageQuery query) {
        return new PageResult<>(List.of(), 0, query);
    }

    /** How many pages the whole list takes; 0 when it is empty. */
    public int totalPages() {
        return (int) Math.ceil((double) totalElements / query.size());
    }

    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        return new PageResult<>(content.stream().<R>map(mapper).toList(), totalElements, query);
    }
}
