package com.platform.backend.modules.audit.application.services;

import com.platform.backend.modules.audit.application.iservices.IAuditService;
import com.platform.backend.modules.audit.application.mappers.AuditEntryMapper;
import com.platform.backend.modules.audit.domain.filters.AuditFilter;
import com.platform.backend.modules.audit.domain.irepositories.IAuditRepository;
import com.platform.backend.modules.audit.presentation.responses.AuditEntryResponse.AuditEntryResponse;
import com.platform.backend.shared.domain.pagination.PageQuery;
import com.platform.backend.shared.domain.pagination.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService implements IAuditService {

    private final IAuditRepository auditRepository;
    private final AuditEntryMapper auditEntryMapper;

    @Override
    public PageResult<AuditEntryResponse> search(AuditFilter filter, PageQuery pageQuery) {
        return auditRepository.search(filter, pageQuery).map(auditEntryMapper::toResponse);
    }
}
