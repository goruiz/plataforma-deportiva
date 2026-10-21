package com.platform.backend.modules.sportcomplexes.infrastructure.persistence.jpa;

import com.platform.backend.modules.sportcomplexes.domain.entities.SportComplexesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SportComplexesJpaRepository extends JpaRepository<SportComplexesEntity, UUID> {

    List<SportComplexesEntity> findAllByDeletedAtIsNull();

    Optional<SportComplexesEntity> findByIdAndDeletedAtIsNull(UUID id);

    // Case-insensitive: "Complejo Norte" and "complejo norte" would be indistinguishable in every complex selector.
    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);

    boolean existsByNameIgnoreCaseAndIdNotAndDeletedAtIsNull(String name, UUID id);
}

