package com.platform.backend.modules.menu.infrastructure.persistence.jpa;

import com.platform.backend.modules.menu.domain.entities.MenuEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuJpaRepository extends JpaRepository<MenuEntity, UUID> {

    // The name breaks the tie between siblings that share an order, so the menu never shuffles between requests.
    List<MenuEntity> findAllByDeletedAtIsNullOrderByOrderAscNameAsc();

    Optional<MenuEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByNameAndDeletedAtIsNull(String name);
}

