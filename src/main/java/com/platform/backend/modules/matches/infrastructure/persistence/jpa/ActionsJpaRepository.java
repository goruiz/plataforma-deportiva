package com.platform.backend.modules.matches.infrastructure.persistence.jpa;

import com.platform.backend.modules.matches.domain.entities.ActionsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActionsJpaRepository extends JpaRepository<ActionsEntity, UUID> {

    List<ActionsEntity> findAllByDeletedAtIsNull();

    Optional<ActionsEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByName(String name);
}
