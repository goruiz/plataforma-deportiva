package com.platform.backend.modules.players.application.mappers;

import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.presentation.requests.CreatePlayerRequest.CreatePlayerRequest;
import com.platform.backend.modules.players.presentation.requests.UpdatePlayerRequest.UpdatePlayerRequest;
import com.platform.backend.modules.players.presentation.responses.PlayerResponse.PlayerResponse;
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
public interface PlayerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "team", ignore = true)       // asignado manualmente en el servicio
    @Mapping(target = "passwordHash", ignore = true) // hasheado manualmente en el servicio
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    PlayersEntity toEntity(CreatePlayerRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "team", ignore = true)       // asignado manualmente en el servicio
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntity(@MappingTarget PlayersEntity entity, UpdatePlayerRequest request);

    // team.id → teamId  |  team.name → teamName
    @Mapping(target = "teamId", source = "team.id")
    @Mapping(target = "teamName", source = "team.name")
    PlayerResponse toResponse(PlayersEntity entity);
}
