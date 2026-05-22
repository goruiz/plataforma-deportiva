package com.platform.backend.modules.permissions.domain.irepositories;

import com.platform.backend.modules.permissions.domain.entities.PermissionsEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IPermissionRepository {

    PermissionsEntity save(PermissionsEntity permission);

    Optional<PermissionsEntity> findById(UUID id);

    List<PermissionsEntity> findAllActive();

    Optional<PermissionsEntity> findActiveById(UUID id);

    void delete(PermissionsEntity permission);

    void hardDelete(PermissionsEntity permission);
}
