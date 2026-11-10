package com.platform.backend.modules.settings.application.mappers;

import com.platform.backend.modules.settings.domain.entities.SystemSettingsEntity;
import com.platform.backend.modules.settings.domain.models.SettingsValues;
import com.platform.backend.modules.settings.presentation.responses.SettingsResponse.SettingsResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE,
        unmappedSourcePolicy = org.mapstruct.ReportingPolicy.IGNORE
)
public interface SystemSettingsMapper {

    SettingsValues toValues(SystemSettingsEntity entity);

    @Mapping(target = "inheritedFromPlatform", ignore = true)
    SettingsResponse toResponse(SystemSettingsEntity entity);
}
