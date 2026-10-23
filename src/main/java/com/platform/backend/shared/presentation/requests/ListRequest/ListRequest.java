package com.platform.backend.shared.presentation.requests.ListRequest;

import lombok.Data;

/**
 * The query string every paginated list accepts. Nothing is validated here on purpose: the limits come from the
 * configuration, so {@code ListQueryResolver} checks them and answers with a code a client can translate.
 */
@Data
public class ListRequest {

    /** Zero-based page index; the first page when missing. */
    private Integer page;

    /** Records per page; the configured default when missing. */
    private Integer size;

    /** Free text; every word must appear in the record. */
    private String search;
}
