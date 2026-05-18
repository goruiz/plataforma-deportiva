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

    boolean existsByName(String name);

    void delete(CategoriesEntity category);

    void hardDelete(CategoriesEntity category);
}
