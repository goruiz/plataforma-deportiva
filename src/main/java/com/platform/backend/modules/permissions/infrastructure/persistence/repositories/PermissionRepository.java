package com.platform.backend.modules.permissions.infrastructure.persistence.repositories;

import com.platform.backend.modules.permissions.domain.entities.PermissionsEntity;
import com.platform.backend.modules.permissions.domain.irepositories.IPermissionRepository;
import com.platform.backend.modules.permissions.infrastructure.persistence.jpa.PermissionsJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PermissionRepository implements IPermissionRepository {

    private final PermissionsJpaRepository permissionsJpaRepository;

    @Override
    public PermissionsEntity save(PermissionsEntity permission) {
        return permissionsJpaRepository.save(permission);
    }

    @Override
    public Optional<PermissionsEntity> findById(UUID id) {
        return permissionsJpaRepository.findById(id);
    }

    @Override
    public List<PermissionsEntity> findAllActive() {
        return permissionsJpaRepository.findAllByDeletedAtIsNull();
    }

    @Override
    public Optional<PermissionsEntity> findActiveById(UUID id) {
        return permissionsJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public void delete(PermissionsEntity permission) {
        permissionsJpaRepository.save(permission);
    }

    @Override
    public void hardDelete(PermissionsEntity permission) {
        permissionsJpaRepository.delete(permission);
    }
}
