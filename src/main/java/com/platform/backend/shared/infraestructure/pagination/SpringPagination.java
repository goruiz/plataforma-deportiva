package com.platform.backend.shared.infraestructure.pagination;

import com.platform.backend.shared.domain.pagination.PageQuery;
import com.platform.backend.shared.domain.pagination.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** The only place that translates the domain's page types to Spring Data's and back, so the domain never imports it. */
public final class SpringPagination {

    private SpringPagination() {}

    public static Pageable toPageable(PageQuery query, Sort sort) {
        return PageRequest.of(query.page(), query.size(), sort);
    }

    public static <T> PageResult<T> toResult(Page<T> page, PageQuery query) {
        return new PageResult<>(page.getContent(), page.getTotalElements(), query);
    }
}
