package com.platform.backend.modules.permissions.application.mappers;

import com.platform.backend.modules.permissions.domain.entities.PermissionsEntity;
import com.platform.backend.modules.permissions.presentation.responses.PermissionResponse.PermissionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE,
        unmappedSourcePolicy = org.mapstruct.ReportingPolicy.IGNORE
)
public interface PermissionMapper {

    @Mapping(target = "roleId", source = "role.id")
    PermissionResponse toResponse(PermissionsEntity entity);
}
