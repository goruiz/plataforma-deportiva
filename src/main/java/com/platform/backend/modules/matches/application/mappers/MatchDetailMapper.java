package com.platform.backend.modules.matches.application.mappers;

import com.platform.backend.modules.matches.domain.entities.MatchDetailsEntity;
import com.platform.backend.modules.matches.presentation.responses.MatchDetailResponse.MatchDetailResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE,
        unmappedSourcePolicy = org.mapstruct.ReportingPolicy.IGNORE
)
public interface MatchDetailMapper {

    @Mapping(target = "matchId", source = "match.id")
    @Mapping(target = "actionId", source = "action.id")
    MatchDetailResponse toResponse(MatchDetailsEntity entity);
}
