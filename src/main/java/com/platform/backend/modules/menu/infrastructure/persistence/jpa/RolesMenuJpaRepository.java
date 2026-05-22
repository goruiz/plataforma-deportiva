package com.platform.backend.modules.menu.infrastructure.persistence.jpa;

import com.platform.backend.modules.menu.domain.entities.RolesMenuEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RolesMenuJpaRepository extends JpaRepository<RolesMenuEntity, UUID> {

    List<RolesMenuEntity> findAllByDeletedAtIsNull();

    Optional<RolesMenuEntity> findByIdAndDeletedAtIsNull(UUID id);
}
