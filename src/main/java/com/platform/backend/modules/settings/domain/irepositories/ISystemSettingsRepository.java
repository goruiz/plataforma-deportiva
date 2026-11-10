package com.platform.backend.modules.settings.domain.irepositories;

import com.platform.backend.modules.settings.domain.entities.SystemSettingsEntity;

import java.util.Optional;
import java.util.UUID;

public interface ISystemSettingsRepository {

    SystemSettingsEntity save(SystemSettingsEntity settings);

    /** The row of one organization; a null organization is the platform defaults. */
    Optional<SystemSettingsEntity> findOfOrganization(UUID organizationId);
}
