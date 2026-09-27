package com.platform.backend.modules.users.infrastructure.persistence.jpa;

import com.platform.backend.modules.users.domain.entities.UsersRolesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsersRolesJpaRepository extends JpaRepository<UsersRolesEntity, UUID> {

    List<UsersRolesEntity> findAllByDeletedAtIsNull();

    Optional<UsersRolesEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByUser_IdAndRole_IdAndDeletedAtIsNull(UUID userId, UUID roleId);

    @Query("select ur.role.id from UsersRolesEntity ur where ur.user.id = :userId and ur.deletedAt is null")
    List<UUID> findActiveRoleIdsByUserId(@Param("userId") UUID userId);
}
