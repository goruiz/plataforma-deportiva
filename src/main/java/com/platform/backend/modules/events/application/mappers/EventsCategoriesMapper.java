package com.platform.backend.modules.events.application.mappers;

import com.platform.backend.modules.events.domain.entities.EventsCategoriesEntity;
import com.platform.backend.modules.events.presentation.responses.EventsCategoriesResponse.EventsCategoriesResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE,
        unmappedSourcePolicy = org.mapstruct.ReportingPolicy.IGNORE
)
public interface EventsCategoriesMapper {

    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "categoryId", source = "category.id")
    EventsCategoriesResponse toResponse(EventsCategoriesEntity entity);
}
