package com.platform.backend.modules.courts.infrastructure.persistence.jpa;

import com.platform.backend.modules.courts.domain.entities.CourtsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourtsJpaRepository extends JpaRepository<CourtsEntity, UUID> {

    List<CourtsEntity> findAllByDeletedAtIsNull();

    Optional<CourtsEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByName(String name);
}
