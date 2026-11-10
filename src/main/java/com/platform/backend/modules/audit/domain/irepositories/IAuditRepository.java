package com.platform.backend.modules.audit.domain.irepositories;

import com.platform.backend.modules.audit.domain.filters.AuditFilter;
import com.platform.backend.modules.audit.domain.models.AuditEntry;
import com.platform.backend.shared.domain.pagination.PageQuery;
import com.platform.backend.shared.domain.pagination.PageResult;

public interface IAuditRepository {

    /**
     * The changes the caller may see (the platform owner sees all, an administrator those of their organization),
     * the most recent first.
     */
    PageResult<AuditEntry> search(AuditFilter filter, PageQuery pageQuery);
}
