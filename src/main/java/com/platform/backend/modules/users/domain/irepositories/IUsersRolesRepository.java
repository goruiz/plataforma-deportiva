package com.platform.backend.modules.users.domain.irepositories;

import com.platform.backend.modules.users.domain.entities.UsersRolesEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IUsersRolesRepository {

    UsersRolesEntity save(UsersRolesEntity usersRoles);

    Optional<UsersRolesEntity> findById(UUID id);

    List<UsersRolesEntity> findAllActive();

    Optional<UsersRolesEntity> findActiveById(UUID id);

    void delete(UsersRolesEntity usersRoles);

    void hardDelete(UsersRolesEntity usersRoles);

    boolean existsActiveByUserAndRole(UUID userId, UUID roleId);

    List<UUID> findActiveRoleIdsByUserId(UUID userId);
}
