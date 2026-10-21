package com.platform.backend.modules.courts.infrastructure.persistence.jpa;

import com.platform.backend.modules.courts.domain.entities.CourtsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourtsJpaRepository extends JpaRepository<CourtsEntity, UUID> {

    List<CourtsEntity> findAllByDeletedAtIsNull();

    Optional<CourtsEntity> findByIdAndDeletedAtIsNull(UUID id);

    // A court name only has to be free inside its own complex ("Cancha 1" exists in every complex), and
    // case-insensitive like the other catalogs. Courts without a complex share one pool of names.
    boolean existsByNameIgnoreCaseAndSportComplex_IdAndDeletedAtIsNull(String name, UUID sportComplexId);

    boolean existsByNameIgnoreCaseAndSportComplexIsNullAndDeletedAtIsNull(String name);

    boolean existsByNameIgnoreCaseAndSportComplex_IdAndIdNotAndDeletedAtIsNull(String name, UUID sportComplexId, UUID id);

    boolean existsByNameIgnoreCaseAndSportComplexIsNullAndIdNotAndDeletedAtIsNull(String name, UUID id);

    boolean existsBySportComplex_IdAndDeletedAtIsNull(UUID sportComplexId);
}

