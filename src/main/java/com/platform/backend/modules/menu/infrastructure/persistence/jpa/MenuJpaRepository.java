package com.platform.backend.modules.menu.infrastructure.persistence.jpa;

import com.platform.backend.modules.menu.domain.entities.MenuEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuJpaRepository extends JpaRepository<MenuEntity, UUID> {

    List<MenuEntity> findAllByDeletedAtIsNullOrderByOrderAsc();

    Optional<MenuEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByNameAndDeletedAtIsNull(String name);
}

