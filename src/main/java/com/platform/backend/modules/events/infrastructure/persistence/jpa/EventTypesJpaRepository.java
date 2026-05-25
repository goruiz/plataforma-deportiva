package com.platform.backend.modules.events.infrastructure.persistence.jpa;

import com.platform.backend.modules.events.domain.entities.EventTypesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventTypesJpaRepository extends JpaRepository<EventTypesEntity, UUID> {

    List<EventTypesEntity> findAllByDeletedAtIsNull();

    Optional<EventTypesEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByName(String name);
}
