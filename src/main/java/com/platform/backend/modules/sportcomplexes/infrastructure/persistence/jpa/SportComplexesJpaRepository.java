package com.platform.backend.modules.sportcomplexes.infrastructure.persistence.jpa;

import com.platform.backend.modules.sportcomplexes.domain.entities.SportComplexesEntity;
import com.platform.backend.shared.infraestructure.tenancy.TenantScopedJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SportComplexesJpaRepository extends TenantScopedJpaRepository<SportComplexesEntity> {

    List<SportComplexesEntity> findAllByDeletedAtIsNull();

    Optional<SportComplexesEntity> findByIdAndDeletedAtIsNull(UUID id);

    // Case-insensitive: "Complejo Norte" and "complejo norte" would be indistinguishable in every complex selector.
    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);

    boolean existsByNameIgnoreCaseAndIdNotAndDeletedAtIsNull(String name, UUID id);
}

