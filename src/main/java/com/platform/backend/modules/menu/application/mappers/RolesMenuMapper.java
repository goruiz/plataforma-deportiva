package com.platform.backend.modules.menu.application.mappers;

import com.platform.backend.modules.menu.domain.entities.RolesMenuEntity;
import com.platform.backend.modules.menu.presentation.responses.RolesMenuResponse.RolesMenuResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE,
        unmappedSourcePolicy = org.mapstruct.ReportingPolicy.IGNORE
)
public interface RolesMenuMapper {

    @Mapping(target = "roleId", source = "role.id")
    @Mapping(target = "menuId", source = "menu.id")
    RolesMenuResponse toResponse(RolesMenuEntity entity);
}
