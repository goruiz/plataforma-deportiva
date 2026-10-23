package com.platform.backend.modules.users.infrastructure.persistence.jpa;

import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.shared.infraestructure.tenancy.TenantScopedJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
