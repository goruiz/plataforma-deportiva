package com.platform.backend.modules.users.application.services;

import com.platform.backend.modules.users.application.iservices.IRolesService;
import com.platform.backend.modules.users.application.mappers.RoleMapper;
import com.platform.backend.modules.users.domain.entities.RolesEntity;
import com.platform.backend.modules.users.domain.irepositories.IRoleRepository;
import com.platform.backend.modules.users.presentation.requests.CreateRoleRequest.CreateRoleRequest;
import com.platform.backend.modules.users.presentation.requests.UpdateRoleRequest.UpdateRoleRequest;
import com.platform.backend.modules.users.presentation.responses.RoleResponse.RoleResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import com.platform.backend.shared.domain.utils.SoftDeleteHelper;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RolesService implements IRolesService {

    private final IRoleRepository roleRepository;
    private final RoleMapper roleMapper;

    @Override
    public List<RoleResponse> getAll() {
        return roleRepository.findAllActive()
                .stream()
                .map(roleMapper::toResponse)
                .toList();
    }

    @Override
    public RoleResponse getById(UUID id) {
        RolesEntity role = roleRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role not found with id: " + id));
        return roleMapper.toResponse(role);
    }

    @Override
    public RoleResponse create(CreateRoleRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            throw new DataIntegrityViolationException("Role name already exists");
        }
        RolesEntity role = roleMapper.toEntity(request);
        return roleMapper.toResponse(roleRepository.save(role));
    }

    @Override
    public RoleResponse update(UUID id, UpdateRoleRequest request) {
        RolesEntity role = roleRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role not found with id: " + id));
        roleMapper.updateEntity(role, request);
        return roleMapper.toResponse(roleRepository.save(role));
    }

    @Override
    public void delete(UUID id) {
        RolesEntity role = roleRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role not found with id: " + id));
        role.setDeletedAt(LocalDateTime.now());
        roleRepository.delete(role);
    }

    @Override
    public void hardDelete(UUID id) {
        RolesEntity role = roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role not found with id: " + id));
        roleRepository.hardDelete(role);
    }
}

