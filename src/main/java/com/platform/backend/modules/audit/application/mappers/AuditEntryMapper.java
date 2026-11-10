package com.platform.backend.modules.audit.application.mappers;

import com.platform.backend.modules.audit.domain.models.AuditEntry;
import com.platform.backend.modules.audit.presentation.responses.AuditEntryResponse.AuditEntryResponse;
import org.mapstruct.Mapper;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE,
        unmappedSourcePolicy = org.mapstruct.ReportingPolicy.IGNORE
)
public interface AuditEntryMapper {

    AuditEntryResponse toResponse(AuditEntry entry);
}
