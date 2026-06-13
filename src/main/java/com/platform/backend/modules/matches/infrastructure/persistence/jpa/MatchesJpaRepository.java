package com.platform.backend.modules.matches.infrastructure.persistence.jpa;

import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchesJpaRepository extends JpaRepository<MatchesEntity, UUID> {

    List<MatchesEntity> findAllByDeletedAtIsNull();

    Optional<MatchesEntity> findByIdAndDeletedAtIsNull(UUID id);

    List<MatchesEntity> findAllByEventIdAndDeletedAtIsNull(UUID eventId);

    List<MatchesEntity> findAllByEventIdAndMatchDateBetweenAndDeletedAtIsNull(
            UUID eventId, LocalDateTime start, LocalDateTime end);
}
