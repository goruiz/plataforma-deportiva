package com.platform.backend.modules.users.domain.irepositories;

import com.platform.backend.modules.users.domain.entities.RolesEntity;
import com.platform.backend.modules.users.domain.entities.UsersRolesEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface IUsersRolesRepository {

    UsersRolesEntity save(UsersRolesEntity usersRoles);

    Optional<UsersRolesEntity> findById(UUID id);

    List<UsersRolesEntity> findAllActive();

    Optional<UsersRolesEntity> findActiveById(UUID id);

    void delete(UsersRolesEntity usersRoles);

    void hardDelete(UsersRolesEntity usersRoles);

    /** Erases every assignment of an account, also the ones already marked as deleted: the foreign key to the account still holds them. */
    void hardDeleteAllOfUser(UUID userId);

    boolean existsActiveByUserAndRole(UUID userId, UUID roleId);

    List<UUID> findActiveRoleIdsByUserId(UUID userId);

    List<UsersRolesEntity> findActiveByUserId(UUID userId);

    /** The active roles of each account, by role name, fetched for all of them at once; an account without roles has no entry. */
    Map<UUID, List<RolesEntity>> findActiveRolesByUserIds(Collection<UUID> userIds);
}
