package com.platform.backend.modules.users.application.mappers;

import com.platform.backend.modules.users.domain.entities.UsersRolesEntity;
import com.platform.backend.modules.users.presentation.responses.UsersRolesResponse.UsersRolesResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE,
        unmappedSourcePolicy = org.mapstruct.ReportingPolicy.IGNORE
)
public interface UsersRolesMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "roleId", source = "role.id")
    UsersRolesResponse toResponse(UsersRolesEntity entity);
}
