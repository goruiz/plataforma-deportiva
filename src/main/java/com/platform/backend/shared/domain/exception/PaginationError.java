package com.platform.backend.shared.domain.exception;

/**
 * Why the paging or search parameters of a list were refused. The constant's name is the stable code clients receive as
 * the reason of the offending field ({@code page}, {@code size} or {@code search}).
 */
public enum PaginationError {
    PAGE_OUT_OF_RANGE,
    SIZE_OUT_OF_RANGE,
    SEARCH_TOO_LONG
}
