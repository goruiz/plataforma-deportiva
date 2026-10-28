package com.platform.backend.modules.users.infrastructure.persistence.jpa;

import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.shared.infraestructure.tenancy.TenantScopedJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface UsersJpaRepository
        extends TenantScopedJpaRepository<UsersEntity>, JpaSpecificationExecutor<UsersEntity> {

    Optional<UsersEntity> findByEmail(String email);

    boolean existsByOrganizationIdAndDeletedAtIsNull(UUID organizationId);

    // Native on purpose: an email or username is taken for the whole platform, and a query of the entity would only
    // see the caller's organization (native queries skip the organization filter).
    @Query(value = "SELECT * FROM users WHERE username = :username LIMIT 1", nativeQuery = true)
    Optional<UsersEntity> findAnyByUsername(@Param("username") String username);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM users WHERE email = :email AND deleted_at IS NULL)", nativeQuery = true)
    boolean existsActiveByEmailInAnyOrganization(@Param("email") String email);

    Optional<UsersEntity> findByIdAndDeletedAtIsNull(UUID id);

    // Native on purpose: the account of a player is moved by whoever changes the player's team, who is an administrator
    // of another organization or none, and so does not see the account through the organization filter. Whoever holds
    // one of the privileged roles (an administrator, the owner) is never moved: their organization is their employer.
    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE users SET organization_id = :organizationId, updated_at = LOCALTIMESTAMP(0)
            WHERE email = :email AND deleted_at IS NULL
              AND organization_id IS DISTINCT FROM :organizationId
              AND NOT EXISTS (SELECT 1 FROM users_roles assignment
                              WHERE assignment.id_user = users.id AND assignment.deleted_at IS NULL
                                AND assignment.id_role IN (:privilegedRoleIds))
            """, nativeQuery = true)
    int assignOrganizationToPlayerAccount(@Param("email") String email,
                                          @Param("organizationId") UUID organizationId,
                                          @Param("privilegedRoleIds") Collection<UUID> privilegedRoleIds);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE users SET organization_id = NULL, updated_at = LOCALTIMESTAMP(0)
            WHERE email = :email AND deleted_at IS NULL AND organization_id IS NOT NULL
              AND NOT EXISTS (SELECT 1 FROM users_roles assignment
                              WHERE assignment.id_user = users.id AND assignment.deleted_at IS NULL
                                AND assignment.id_role IN (:privilegedRoleIds))
            """, nativeQuery = true)
    int clearOrganizationOfPlayerAccount(@Param("email") String email,
                                         @Param("privilegedRoleIds") Collection<UUID> privilegedRoleIds);
}
