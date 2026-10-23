package com.platform.backend.modules.events.infrastructure.persistence.jpa;

import com.platform.backend.modules.events.domain.entities.EventsCategoriesEntity;
import com.platform.backend.shared.infraestructure.tenancy.TenantScopedJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventsCategoriesJpaRepository extends TenantScopedJpaRepository<EventsCategoriesEntity> {

    List<EventsCategoriesEntity> findAllByDeletedAtIsNull();

    Optional<EventsCategoriesEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByCategoryIdAndDeletedAtIsNull(UUID categoryId);
}
