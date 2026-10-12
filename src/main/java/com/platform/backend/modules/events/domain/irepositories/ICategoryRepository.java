package com.platform.backend.modules.events.domain.irepositories;

import com.platform.backend.modules.events.domain.entities.CategoriesEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ICategoryRepository {

    CategoriesEntity save(CategoriesEntity category);

    Optional<CategoriesEntity> findById(UUID id);

    List<CategoriesEntity> findAllActive();

    Optional<CategoriesEntity> findActiveById(UUID id);

    /** Names are compared ignoring case. */
    boolean existsByName(String name);

    /** True when an active category other than {@code id} already uses {@code name} (ignoring case). */
    boolean existsByNameExcludingId(String name, UUID id);

    void delete(CategoriesEntity category);

    void hardDelete(CategoriesEntity category);
}
