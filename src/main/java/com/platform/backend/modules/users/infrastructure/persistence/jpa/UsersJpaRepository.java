package com.platform.backend.modules.users.infrastructure.persistence.jpa;

import com.platform.backend.modules.users.domain.entities.UsersEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsersJpaRepository
        extends JpaRepository<UsersEntity, UUID> {

    Optional<UsersEntity> findByEmail(String email);

    Optional<UsersEntity> findByUsername(String username);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    List<UsersEntity> findAllByDeletedAtIsNull();

    Optional<UsersEntity> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
            select u from UsersEntity u
            where u.deletedAt is null
              and exists (select 1 from UsersRolesEntity ur
                          where ur.user.id = u.id and ur.role.id = :roleId and ur.deletedAt is null)
            order by u.firstName, u.lastName, u.email
            """)
    List<UsersEntity> findAllActiveWithActiveRole(@Param("roleId") UUID roleId);

    @Query("""
            select u from UsersEntity u
            where u.deletedAt is null
              and not exists (select 1 from UsersRolesEntity ur
                              where ur.user.id = u.id and ur.role.id = :roleId and ur.deletedAt is null)
            order by u.firstName, u.lastName, u.email
            """)
    List<UsersEntity> findAllActiveWithoutActiveRole(@Param("roleId") UUID roleId);
}
