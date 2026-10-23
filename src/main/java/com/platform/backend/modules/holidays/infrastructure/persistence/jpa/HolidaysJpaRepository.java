package com.platform.backend.modules.holidays.infrastructure.persistence.jpa;

import com.platform.backend.modules.holidays.domain.entities.HolidaysEntity;
import com.platform.backend.shared.infraestructure.tenancy.TenantScopedJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HolidaysJpaRepository extends TenantScopedJpaRepository<HolidaysEntity> {

    List<HolidaysEntity> findAllByDeletedAtIsNull();

    Optional<HolidaysEntity> findByIdAndDeletedAtIsNull(UUID id);

    List<HolidaysEntity> findAllByOrganizationIdAndDeletedAtIsNull(UUID organizationId);

    List<HolidaysEntity> findAllByOrganizationIdIsNullAndDeletedAtIsNull();
}
