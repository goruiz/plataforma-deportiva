package com.platform.backend.modules.users.infrastructure.persistence.jpa;

import com.platform.backend.modules.users.domain.entities.UsersRolesEntity;
import com.platform.backend.shared.infraestructure.tenancy.TenantScopedJpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsersRolesJpaRepository extends TenantScopedJpaRepository<UsersRolesEntity> {

    List<UsersRolesEntity> findAllByDeletedAtIsNull();

    Optional<UsersRolesEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByUser_IdAndRole_IdAndDeletedAtIsNull(UUID userId, UUID roleId);

    @Query("select ur.role.id from UsersRolesEntity ur where ur.user.id = :userId and ur.deletedAt is null")
    List<UUID> findActiveRoleIdsByUserId(@Param("userId") UUID userId);

    List<UsersRolesEntity> findAllByUser_IdAndDeletedAtIsNull(UUID userId);

    /** (user id, role) pairs of several accounts in one query: the roles shown next to each row of a page. */
    @Query("""
            select ur.user.id, ur.role
            from UsersRolesEntity ur
            where ur.user.id in :userIds and ur.deletedAt is null and ur.role.deletedAt is null
            order by ur.role.name
            """)
    List<Object[]> findActiveRolesByUserIds(@Param("userIds") Collection<UUID> userIds);
}
