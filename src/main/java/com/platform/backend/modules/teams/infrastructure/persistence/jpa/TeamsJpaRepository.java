package com.platform.backend.modules.teams.infrastructure.persistence.jpa;

import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.shared.infraestructure.tenancy.TenantScopedJpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamsJpaRepository extends TenantScopedJpaRepository<TeamsEntity> {

    List<TeamsEntity> findAllByDeletedAtIsNull();

    Optional<TeamsEntity> findByIdAndDeletedAtIsNull(UUID id);

    // Case-insensitive: "Tigres" and "tigres" would be indistinguishable in every team list.
    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);

    boolean existsByNameIgnoreCaseAndIdNotAndDeletedAtIsNull(String name, UUID id);

    boolean existsByCategoryIdAndDeletedAtIsNull(UUID categoryId);

    // Native on purpose, like the lookups of an email or a username: a player without a team sees no team of any
    // organization through the filter, and a join code is exactly what lets them reach one.
    @Query(value = "SELECT EXISTS (SELECT 1 FROM teams WHERE join_code = :joinCode AND deleted_at IS NULL)", nativeQuery = true)
    boolean existsActiveByJoinCodeInAnyOrganization(@Param("joinCode") String joinCode);

    @Query(value = "SELECT * FROM teams WHERE join_code = :joinCode AND deleted_at IS NULL LIMIT 1", nativeQuery = true)
    Optional<TeamsEntity> findActiveByJoinCodeInAnyOrganization(@Param("joinCode") String joinCode);

    boolean existsByOrganizationIdAndDeletedAtIsNull(UUID organizationId);

    @Query("SELECT t FROM TeamsEntity t WHERE t.id IN :ids AND t.deletedAt IS NULL")
    List<TeamsEntity> findAllActiveByIds(@Param("ids") Collection<UUID> ids);



}

