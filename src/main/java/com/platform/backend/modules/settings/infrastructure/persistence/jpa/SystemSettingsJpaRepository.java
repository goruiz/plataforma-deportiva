package com.platform.backend.modules.settings.infrastructure.persistence.jpa;

import com.platform.backend.modules.settings.domain.entities.SystemSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SystemSettingsJpaRepository extends JpaRepository<SystemSettingsEntity, UUID> {

    Optional<SystemSettingsEntity> findByOrganizationIdAndDeletedAtIsNull(UUID organizationId);

    Optional<SystemSettingsEntity> findByOrganizationIdIsNullAndDeletedAtIsNull();
}
