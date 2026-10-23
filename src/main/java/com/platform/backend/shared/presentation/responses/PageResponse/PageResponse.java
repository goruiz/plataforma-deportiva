package com.platform.backend.shared.presentation.responses.PageResponse;

import com.platform.backend.shared.domain.pagination.PageResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** The {@code data} of every paginated list: the page's records and where it sits in the whole list. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;
    /** Zero-based index of this page. */
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public static <T> PageResponse<T> from(PageResult<T> result) {
        return new PageResponse<>(
                result.content(),
                result.query().page(),
                result.query().size(),
                result.totalElements(),
                result.totalPages());
    }
}
