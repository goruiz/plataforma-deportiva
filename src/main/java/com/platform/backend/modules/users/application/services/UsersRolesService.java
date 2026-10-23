package com.platform.backend.modules.users.application.services;

import com.platform.backend.modules.users.application.iservices.IUsersRolesService;
import com.platform.backend.modules.users.application.mappers.UsersRolesMapper;
import com.platform.backend.modules.users.domain.entities.RolesEntity;
import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.domain.entities.UsersRolesEntity;
import com.platform.backend.modules.users.domain.irepositories.IRoleRepository;
import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.modules.users.domain.irepositories.IUsersRolesRepository;
import com.platform.backend.modules.users.infrastructure.configuration.security.PlatformOwnerRole;
import com.platform.backend.modules.users.presentation.requests.CreateUsersRolesRequest.CreateUsersRolesRequest;
import com.platform.backend.modules.users.presentation.responses.UsersRolesResponse.UsersRolesResponse;
import com.platform.backend.shared.infraestructure.tenancy.CurrentTenant;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsersRolesService implements IUsersRolesService {

    private final IUsersRolesRepository usersRolesRepository;
    private final IUserRepository userRepository;
    private final IRoleRepository roleRepository;
    private final UsersRolesMapper usersRolesMapper;
    private final PlatformOwnerRole platformOwnerRole;

    @Override
    public List<UsersRolesResponse> getAll() {
        return usersRolesRepository.findAllActive()
                .stream()
                .map(usersRolesMapper::toResponse)
                .toList();
    }

    @Override
    public List<UsersRolesResponse> getByUserId(UUID userId) {
        return usersRolesRepository.findActiveByUserId(userId)
                .stream()
                .map(usersRolesMapper::toResponse)
                .toList();
    }

    @Override
    public UsersRolesResponse getById(UUID id) {
        UsersRolesEntity usersRoles = usersRolesRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("User-role assignment not found with id: " + id));
        return usersRolesMapper.toResponse(usersRoles);
    }

    @Override
    public UsersRolesResponse create(CreateUsersRolesRequest request) {
        UsersEntity user = userRepository.findActiveById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + request.getUserId()));
        RolesEntity role = roleRepository.findActiveById(request.getRoleId())
                .orElseThrow(() -> new EntityNotFoundException("Role not found with id: " + request.getRoleId()));
        requireOwnerToHandleOwnerRole(role.getId());

        UsersRolesEntity usersRoles = new UsersRolesEntity();
        usersRoles.setUser(user);
        usersRoles.setRole(role);

        return usersRolesMapper.toResponse(usersRolesRepository.save(usersRoles));
    }

    @Override
    public void delete(UUID id) {
        UsersRolesEntity usersRoles = usersRolesRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("User-role assignment not found with id: " + id));
        requireOwnerToHandleOwnerRole(usersRoles.getRole().getId());
        usersRoles.setDeletedAt(LocalDateTime.now());
        usersRolesRepository.delete(usersRoles);
    }

    @Override
    public void hardDelete(UUID id) {
        UsersRolesEntity usersRoles = usersRolesRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User-role assignment not found with id: " + id));
        requireOwnerToHandleOwnerRole(usersRoles.getRole().getId());
        usersRolesRepository.hardDelete(usersRoles);
    }

    /** The platform owner role is how the system's seller is told apart from its customers: customers must not be able to grant it. */
    private void requireOwnerToHandleOwnerRole(UUID roleId) {
        if (platformOwnerRole.isOwnerRole(roleId) && !CurrentTenant.isPlatformOwner()) {
            throw new AccessDeniedException("Only the platform owner can assign or remove the platform owner role.");
        }
    }
}
