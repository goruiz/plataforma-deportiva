package com.platform.backend.modules.menu.domain.irepositories;

import com.platform.backend.modules.menu.domain.entities.RolesMenuEntity;

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
}
