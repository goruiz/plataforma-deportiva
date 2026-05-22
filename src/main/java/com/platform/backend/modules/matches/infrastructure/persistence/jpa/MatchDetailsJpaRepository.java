package com.platform.backend.modules.matches.infrastructure.persistence.jpa;

import com.platform.backend.modules.matches.domain.entities.MatchDetailsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchDetailsJpaRepository extends JpaRepository<MatchDetailsEntity, UUID> {

    List<MatchDetailsEntity> findAllByDeletedAtIsNull();

    Optional<MatchDetailsEntity> findByIdAndDeletedAtIsNull(UUID id);
}
