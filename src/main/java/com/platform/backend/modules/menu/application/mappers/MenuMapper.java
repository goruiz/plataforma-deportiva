package com.platform.backend.modules.menu.application.mappers;

import com.platform.backend.modules.menu.domain.entities.MenuEntity;
import com.platform.backend.modules.menu.presentation.requests.CreateMenuRequest.CreateMenuRequest;
import com.platform.backend.modules.menu.presentation.requests.UpdateMenuRequest.UpdateMenuRequest;
import com.platform.backend.modules.menu.presentation.responses.MenuResponse.MenuResponse;
import com.platform.backend.modules.menu.presentation.responses.MenuResponse.MenuTreeResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE,
        unmappedSourcePolicy = org.mapstruct.ReportingPolicy.IGNORE
)
public interface MenuMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    MenuEntity toEntity(CreateMenuRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntity(@MappingTarget MenuEntity entity, UpdateMenuRequest request);

    MenuResponse toResponse(MenuEntity entity);

    MenuTreeResponse toTreeResponse(MenuEntity entity);
}
