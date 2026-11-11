package com.platform.backend.modules.events.infrastructure.persistence.jpa;

import com.platform.backend.modules.events.domain.entities.CategoriesEntity;
import com.platform.backend.shared.infraestructure.tenancy.TenantScopedJpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoriesJpaRepository extends TenantScopedJpaRepository<CategoriesEntity> {

    // Native on purpose: the team a person joins may belong to another organization, whose categories the organization
    // filter hides, and its rules still apply to them. Only the rules are read.
    @Query(value = "SELECT id AS id, gender AS gender, min_age AS minAge, max_age AS maxAge FROM categories "
            + "WHERE id IN (:ids) AND deleted_at IS NULL", nativeQuery = true)
    List<CategoryRules> findRulesByIds(@Param("ids") Collection<UUID> ids);

    /** Just the rules of a category. */
    interface CategoryRules {
        UUID getId();

        String getGender();

        Integer getMinAge();

        Integer getMaxAge();
    }

    List<CategoriesEntity> findAllByDeletedAtIsNull();

    Optional<CategoriesEntity> findByIdAndDeletedAtIsNull(UUID id);

    // Case-insensitive: "Sub-15" and "sub-15" would be indistinguishable in every category selector.
    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);

    boolean existsByNameIgnoreCaseAndIdNotAndDeletedAtIsNull(String name, UUID id);
}
