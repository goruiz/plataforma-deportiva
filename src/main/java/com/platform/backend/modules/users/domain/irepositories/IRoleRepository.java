package com.platform.backend.modules.users.domain.irepositories;

import com.platform.backend.modules.users.domain.entities.RolesEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IRoleRepository {

    RolesEntity save(RolesEntity role);

    Optional<RolesEntity> findById(UUID id);

    List<RolesEntity> findAllActive();

    Optional<RolesEntity> findActiveById(UUID id);

    boolean existsByName(String name);

    void delete(RolesEntity role);

    void hardDelete(RolesEntity role);
}
