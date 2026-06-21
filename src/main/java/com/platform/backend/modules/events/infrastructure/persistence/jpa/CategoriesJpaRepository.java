package com.platform.backend.modules.events.infrastructure.persistence.jpa;

import com.platform.backend.modules.events.domain.entities.CategoriesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoriesJpaRepository extends JpaRepository<CategoriesEntity, UUID> {

    List<CategoriesEntity> findAllByDeletedAtIsNull();

    Optional<CategoriesEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByNameAndDeletedAtIsNull(String name);
}

