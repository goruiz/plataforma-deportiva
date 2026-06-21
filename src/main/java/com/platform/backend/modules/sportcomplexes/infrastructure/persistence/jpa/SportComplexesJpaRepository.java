package com.platform.backend.modules.sportcomplexes.infrastructure.persistence.jpa;

import com.platform.backend.modules.sportcomplexes.domain.entities.SportComplexesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SportComplexesJpaRepository extends JpaRepository<SportComplexesEntity, UUID> {

    List<SportComplexesEntity> findAllByDeletedAtIsNull();

    Optional<SportComplexesEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByNameAndDeletedAtIsNull(String name);
}

