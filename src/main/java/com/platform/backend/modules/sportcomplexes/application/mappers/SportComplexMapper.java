package com.platform.backend.modules.sportcomplexes.application.mappers;

import com.platform.backend.modules.sportcomplexes.domain.entities.SportComplexesEntity;
import com.platform.backend.modules.sportcomplexes.presentation.requests.CreateSportComplexRequest.CreateSportComplexRequest;
import com.platform.backend.modules.sportcomplexes.presentation.requests.UpdateSportComplexRequest.UpdateSportComplexRequest;
import com.platform.backend.modules.sportcomplexes.presentation.responses.SportComplexResponse.SportComplexResponse;
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
public interface SportComplexMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    SportComplexesEntity toEntity(CreateSportComplexRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntity(@MappingTarget SportComplexesEntity entity, UpdateSportComplexRequest request);

    SportComplexResponse toResponse(SportComplexesEntity entity);
}
