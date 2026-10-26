package com.platform.backend.modules.players.application.mappers;

import com.platform.backend.modules.players.domain.entities.TeamInvitationsEntity;
import com.platform.backend.modules.players.presentation.responses.TeamInvitationResponse.TeamInvitationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE,
        unmappedSourcePolicy = org.mapstruct.ReportingPolicy.IGNORE
)
public interface TeamInvitationMapper {

    @Mapping(target = "teamName", ignore = true) // the invitation only holds the team id: the service names it
    TeamInvitationResponse toResponse(TeamInvitationsEntity entity);
}
