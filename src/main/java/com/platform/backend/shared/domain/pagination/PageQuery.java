package com.platform.backend.shared.domain.pagination;

/**
 * Which slice of a list a caller asks for: the zero-based page and how many records a page holds. Whoever builds one
 * (see {@code ListQueryResolver}) has already checked the limits, so a bad value here is a programming error.
 */
public record PageQuery(int page, int size) {

    public PageQuery {
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("A page query needs a page >= 0 and a size >= 1");
        }
    }

    public long offset() {
        return (long) page * size;
    }
}
