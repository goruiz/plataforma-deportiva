package com.platform.backend.modules.teams.infrastructure.persistence.jpa;

import com.platform.backend.modules.teams.domain.entities.TeamsEventsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamsEventsJpaRepository extends JpaRepository<TeamsEventsEntity, UUID> {

    List<TeamsEventsEntity> findAllByDeletedAtIsNull();

    Optional<TeamsEventsEntity> findByIdAndDeletedAtIsNull(UUID id);
}
