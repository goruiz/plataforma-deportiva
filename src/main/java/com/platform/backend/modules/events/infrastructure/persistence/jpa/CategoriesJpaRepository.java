package com.platform.backend.modules.events.infrastructure.persistence.jpa;

import com.platform.backend.modules.events.domain.entities.CategoriesEntity;
import com.platform.backend.shared.infraestructure.tenancy.TenantScopedJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoriesJpaRepository extends TenantScopedJpaRepository<CategoriesEntity> {

    List<CategoriesEntity> findAllByDeletedAtIsNull();

    Optional<CategoriesEntity> findByIdAndDeletedAtIsNull(UUID id);

    // Case-insensitive: "Sub-15" and "sub-15" would be indistinguishable in every category selector.
    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);

    boolean existsByNameIgnoreCaseAndIdNotAndDeletedAtIsNull(String name, UUID id);
}
