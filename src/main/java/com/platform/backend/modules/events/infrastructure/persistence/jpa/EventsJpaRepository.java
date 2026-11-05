package com.platform.backend.modules.events.infrastructure.persistence.jpa;

import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.shared.infraestructure.tenancy.TenantScopedJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventsJpaRepository
        extends TenantScopedJpaRepository<EventsEntity>, JpaSpecificationExecutor<EventsEntity> {

    List<EventsEntity> findAllByDeletedAtIsNull();

    @Query("SELECT e FROM EventsEntity e WHERE e.id IN :ids AND e.deletedAt IS NULL")
    List<EventsEntity> findAllActiveByIds(@Param("ids") Collection<UUID> ids);

    Optional<EventsEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByNameAndDeletedAtIsNull(String name);

    List<EventsEntity> findAllByEventTypeIdAndDeletedAtIsNull(UUID eventTypeId);

    List<EventsEntity> findAllByEventTypeIdAndCreatedByAndDeletedAtIsNull(UUID eventTypeId, UUID createdBy);

    boolean existsByEventFormatIdAndDeletedAtIsNull(UUID eventFormatId);

    boolean existsByOrganizationIdAndDeletedAtIsNull(UUID organizationId);
}
