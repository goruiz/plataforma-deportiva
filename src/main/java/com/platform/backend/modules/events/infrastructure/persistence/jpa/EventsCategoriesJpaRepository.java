package com.platform.backend.modules.events.infrastructure.persistence.jpa;

import com.platform.backend.modules.events.domain.entities.EventsCategoriesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventsCategoriesJpaRepository extends JpaRepository<EventsCategoriesEntity, UUID> {

    List<EventsCategoriesEntity> findAllByDeletedAtIsNull();

    Optional<EventsCategoriesEntity> findByIdAndDeletedAtIsNull(UUID id);
}
