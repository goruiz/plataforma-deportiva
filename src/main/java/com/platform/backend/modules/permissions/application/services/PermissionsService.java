package com.platform.backend.modules.permissions.application.services;

import com.platform.backend.modules.permissions.application.iservices.IPermissionsService;
import com.platform.backend.modules.permissions.application.mappers.PermissionMapper;
import com.platform.backend.modules.permissions.domain.entities.PermissionsEntity;
import com.platform.backend.modules.permissions.domain.irepositories.IPermissionRepository;
import com.platform.backend.modules.permissions.presentation.requests.CreatePermissionRequest.CreatePermissionRequest;
import com.platform.backend.modules.permissions.presentation.requests.UpdatePermissionRequest.UpdatePermissionRequest;
import com.platform.backend.modules.permissions.presentation.responses.PermissionResponse.PermissionResponse;
import com.platform.backend.modules.users.domain.entities.RolesEntity;
import com.platform.backend.modules.users.domain.irepositories.IRoleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PermissionsService implements IPermissionsService {

    private final IPermissionRepository permissionRepository;
    private final IRoleRepository roleRepository;
    private final PermissionMapper permissionMapper;

    @Override
    public List<PermissionResponse> getAll() {
        return permissionRepository.findAllActive()
                .stream()
                .map(permissionMapper::toResponse)
                .toList();
    }

    @Override
    public PermissionResponse getById(UUID id) {
        PermissionsEntity permission = permissionRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Permission not found with id: " + id));
        return permissionMapper.toResponse(permission);
    }

    @Override
    public PermissionResponse create(CreatePermissionRequest request) {
        RolesEntity role = roleRepository.findActiveById(request.getRoleId())
                .orElseThrow(() -> new EntityNotFoundException("Role not found with id: " + request.getRoleId()));

        PermissionsEntity permission = new PermissionsEntity();
        permission.setName(request.getName());
        permission.setRole(role);

        return permissionMapper.toResponse(permissionRepository.save(permission));
    }

    @Override
    public PermissionResponse update(UUID id, UpdatePermissionRequest request) {
        PermissionsEntity permission = permissionRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Permission not found with id: " + id));

        if (request.getName() != null) permission.setName(request.getName());
        if (request.getRoleId() != null) {
            RolesEntity role = roleRepository.findActiveById(request.getRoleId())
                    .orElseThrow(() -> new EntityNotFoundException("Role not found with id: " + request.getRoleId()));
            permission.setRole(role);
        }

        return permissionMapper.toResponse(permissionRepository.save(permission));
    }

    @Override
    public void delete(UUID id) {
        PermissionsEntity permission = permissionRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Permission not found with id: " + id));
        permission.setDeletedAt(LocalDateTime.now());
        permissionRepository.delete(permission);
    }

    @Override
    public void hardDelete(UUID id) {
        PermissionsEntity permission = permissionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Permission not found with id: " + id));
        permissionRepository.hardDelete(permission);
    }
}
