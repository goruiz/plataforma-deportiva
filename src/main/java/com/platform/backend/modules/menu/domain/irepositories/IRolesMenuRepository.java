package com.platform.backend.modules.menu.domain.irepositories;

import com.platform.backend.modules.menu.domain.entities.RolesMenuEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IRolesMenuRepository {

    RolesMenuEntity save(RolesMenuEntity rolesMenu);

    Optional<RolesMenuEntity> findById(UUID id);

    List<RolesMenuEntity> findAllActive();

    Optional<RolesMenuEntity> findActiveById(UUID id);

    void delete(RolesMenuEntity rolesMenu);

    void hardDelete(RolesMenuEntity rolesMenu);

    List<RolesMenuEntity> findActiveVisibleByRoleIds(List<UUID> roleIds);

    /** Every active assignment of the role, visible or not. */
    List<RolesMenuEntity> findActiveByRoleId(UUID roleId);

    /** Every active assignment of those menu items, whichever role holds it. */
    List<RolesMenuEntity> findActiveByMenuIds(Collection<UUID> menuIds);
}
