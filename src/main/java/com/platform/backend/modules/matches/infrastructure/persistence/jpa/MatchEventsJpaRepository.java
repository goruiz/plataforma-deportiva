package com.platform.backend.modules.matches.infrastructure.persistence.jpa;

import com.platform.backend.modules.matches.domain.entities.MatchEventsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchEventsJpaRepository extends JpaRepository<MatchEventsEntity, UUID> {

    List<MatchEventsEntity> findAllByDeletedAtIsNull();

    Optional<MatchEventsEntity> findByIdAndDeletedAtIsNull(UUID id);
}
