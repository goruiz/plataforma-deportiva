package com.platform.backend.modules.users.infrastructure.persistence.jpa;

import com.platform.backend.modules.users.domain.entities.RolesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RolesJpaRepository extends JpaRepository<RolesEntity, UUID> {

    List<RolesEntity> findAllByDeletedAtIsNull();

    Optional<RolesEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByName(String name);
}
