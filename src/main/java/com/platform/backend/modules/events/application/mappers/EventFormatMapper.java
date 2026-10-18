package com.platform.backend.modules.events.application.mappers;

import com.platform.backend.modules.events.domain.entities.EventFormatsEntity;
import com.platform.backend.modules.events.domain.enums.TiebreakerCriterion;
import com.platform.backend.modules.events.presentation.requests.CreateEventFormatRequest.CreateEventFormatRequest;
import com.platform.backend.modules.events.presentation.requests.UpdateEventFormatRequest.UpdateEventFormatRequest;
import com.platform.backend.modules.events.presentation.responses.EventFormatResponse.EventFormatResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE,
        unmappedSourcePolicy = org.mapstruct.ReportingPolicy.IGNORE
)
public interface EventFormatMapper {

    String TIEBREAKERS_TO_STORED = "tiebreakersToStored";
    String TIEBREAKERS_FROM_STORED = "tiebreakersFromStored";

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "events", ignore = true)
    @Mapping(target = "tiebreakers", qualifiedByName = TIEBREAKERS_TO_STORED)
    EventFormatsEntity toEntity(CreateEventFormatRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "events", ignore = true)
    @Mapping(target = "tiebreakers", qualifiedByName = TIEBREAKERS_TO_STORED)
    void updateEntity(@MappingTarget EventFormatsEntity entity, UpdateEventFormatRequest request);

    @Mapping(target = "tiebreakers", qualifiedByName = TIEBREAKERS_FROM_STORED)
    EventFormatResponse toResponse(EventFormatsEntity entity);

    @Named(TIEBREAKERS_TO_STORED)
    default String tiebreakersToStored(List<TiebreakerCriterion> tiebreakers) {
        return TiebreakerCriterion.toStored(tiebreakers);
    }

    @Named(TIEBREAKERS_FROM_STORED)
    default List<TiebreakerCriterion> tiebreakersFromStored(String stored) {
        return TiebreakerCriterion.fromStored(stored);
    }
}
