package com.platform.backend.modules.events.infrastructure.persistence.jpa;

import com.platform.backend.modules.events.domain.entities.EventFormatsEntity;
import com.platform.backend.shared.infraestructure.tenancy.TenantScopedJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventFormatsJpaRepository extends TenantScopedJpaRepository<EventFormatsEntity> {

    List<EventFormatsEntity> findAllByDeletedAtIsNull();

    Optional<EventFormatsEntity> findByIdAndDeletedAtIsNull(UUID id);

    // Case-insensitive: "Liga" and "liga" would be indistinguishable in the format selector.
    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);

    boolean existsByNameIgnoreCaseAndIdNotAndDeletedAtIsNull(String name, UUID id);
}
