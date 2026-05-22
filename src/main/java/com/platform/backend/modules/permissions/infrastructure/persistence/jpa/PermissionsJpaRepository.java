package com.platform.backend.modules.permissions.infrastructure.persistence.jpa;

import com.platform.backend.modules.permissions.domain.entities.PermissionsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionsJpaRepository extends JpaRepository<PermissionsEntity, UUID> {

    List<PermissionsEntity> findAllByDeletedAtIsNull();

    Optional<PermissionsEntity> findByIdAndDeletedAtIsNull(UUID id);
}
