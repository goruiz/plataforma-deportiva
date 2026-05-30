package com.platform.backend.modules.teams.application.mappers;

import com.platform.backend.modules.teams.domain.entities.TeamsEventsEntity;
import com.platform.backend.modules.teams.presentation.responses.TeamsEventsResponse.TeamsEventsResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE,
        unmappedSourcePolicy = org.mapstruct.ReportingPolicy.IGNORE
)
public interface TeamsEventsMapper {

    @Mapping(target = "teamId", source = "team.id")
    @Mapping(target = "teamName", source = "team.name")
    @Mapping(target = "eventId", source = "event.id")
    TeamsEventsResponse toResponse(TeamsEventsEntity entity);
}
