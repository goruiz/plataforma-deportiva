package com.platform.backend.modules.settings.infrastructure.persistence.repositories;

import com.platform.backend.modules.settings.domain.entities.SystemSettingsEntity;
import com.platform.backend.modules.settings.domain.irepositories.ISystemSettingsRepository;
import com.platform.backend.modules.settings.infrastructure.persistence.jpa.SystemSettingsJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SystemSettingsRepository implements ISystemSettingsRepository {

    private final SystemSettingsJpaRepository systemSettingsJpaRepository;

    @Override
    public SystemSettingsEntity save(SystemSettingsEntity settings) {
        return systemSettingsJpaRepository.save(settings);
    }

    @Override
    public Optional<SystemSettingsEntity> findOfOrganization(UUID organizationId) {
        return organizationId == null
                ? systemSettingsJpaRepository.findByOrganizationIdIsNullAndDeletedAtIsNull()
                : systemSettingsJpaRepository.findByOrganizationIdAndDeletedAtIsNull(organizationId);
    }
}
