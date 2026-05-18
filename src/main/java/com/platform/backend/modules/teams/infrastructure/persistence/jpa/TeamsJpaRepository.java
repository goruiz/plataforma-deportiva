package com.platform.backend.modules.teams.infrastructure.persistence.jpa;

import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamsJpaRepository extends JpaRepository<TeamsEntity, UUID> {

    List<TeamsEntity> findAllByDeletedAtIsNull();

    Optional<TeamsEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByName(String name);
}
