package com.platform.backend.modules.events.domain.irepositories;

import com.platform.backend.modules.events.domain.entities.CategoriesEntity;
import com.platform.backend.modules.events.domain.models.CategoryRequirements;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ICategoryRepository {

    CategoriesEntity save(CategoriesEntity category);

    Optional<CategoriesEntity> findById(UUID id);

    List<CategoriesEntity> findAllActive();

    Optional<CategoriesEntity> findActiveById(UUID id);

    /**
     * What each of the categories asks of its players, whichever organization they belong to: the team of another
     * organization a person joins has a category their own organization does not see, and its rules still apply to them.
     * Only the rules are read, never the rest of the category.
     */
    Map<UUID, CategoryRequirements> findRequirementsByIds(Collection<UUID> ids);

    /** Names are compared ignoring case. */
    boolean existsByName(String name);

    /** True when an active category other than {@code id} already uses {@code name} (ignoring case). */
    boolean existsByNameExcludingId(String name, UUID id);

    void delete(CategoriesEntity category);

    void hardDelete(CategoriesEntity category);
}
