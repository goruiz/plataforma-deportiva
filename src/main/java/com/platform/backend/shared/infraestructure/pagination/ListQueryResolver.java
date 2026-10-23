package com.platform.backend.shared.infraestructure.pagination;

import com.platform.backend.shared.domain.exception.PaginationError;
import com.platform.backend.shared.domain.pagination.PageQuery;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Turns the paging and search parameters of a list request into values the application can trust, or refuses them
 * with a 400 that names the offending field ({@code data: {size: SIZE_OUT_OF_RANGE}}). The limits are configuration
 * ({@code app.pagination.*}): an unbounded page size would let one request read the whole table.
 */
@Component
public class ListQueryResolver {

    private final int defaultSize;
    private final int maxSize;
    private final int maxSearchLength;
    private final int maxSearchTerms;

    public ListQueryResolver(
            @Value("${app.pagination.default-size:20}") int defaultSize,
            @Value("${app.pagination.max-size:100}") int maxSize,
            @Value("${app.pagination.max-search-length:100}") int maxSearchLength,
            @Value("${app.pagination.max-search-terms:5}") int maxSearchTerms) {
        this.defaultSize = defaultSize;
        this.maxSize = maxSize;
        this.maxSearchLength = maxSearchLength;
        this.maxSearchTerms = maxSearchTerms;
    }

    /** The first page of the default size when a value is missing; a negative page or a size outside 1..max is a 400. */
    public PageQuery pageQuery(Integer page, Integer size) {
        int resolvedPage = page == null ? 0 : page;
        int resolvedSize = size == null ? defaultSize : size;
        if (resolvedPage < 0) {
            throw new FieldValidationException("page", PaginationError.PAGE_OUT_OF_RANGE.name());
        }
        if (resolvedSize < 1 || resolvedSize > maxSize) {
            throw new FieldValidationException("size", PaginationError.SIZE_OUT_OF_RANGE.name());
        }
        return new PageQuery(resolvedPage, resolvedSize);
    }

    /**
     * The words to look for: lower-cased, in the order typed, without repeats and capped at the configured number
     * (the extra ones are dropped, they would only slow the query down). A blank search means "no search" and an
     * over-long one is a 400.
     */
    public List<String> searchTerms(String search) {
        if (!StringUtils.hasText(search)) {
            return List.of();
        }
        if (search.length() > maxSearchLength) {
            throw new FieldValidationException("search", PaginationError.SEARCH_TOO_LONG.name());
        }
        return Arrays.stream(search.trim().toLowerCase(Locale.ROOT).split("\\s+"))
                .distinct()
                .limit(maxSearchTerms)
                .toList();
    }
}
