package com.platform.backend.modules.audit.application.iservices;

import com.platform.backend.modules.audit.domain.filters.AuditFilter;
import com.platform.backend.modules.audit.presentation.responses.AuditEntryResponse.AuditEntryResponse;
import com.platform.backend.shared.domain.pagination.PageQuery;
import com.platform.backend.shared.domain.pagination.PageResult;

public interface IAuditService {

    /** The changes the caller may see, the most recent first, a page at a time. */
    PageResult<AuditEntryResponse> search(AuditFilter filter, PageQuery pageQuery);
}
