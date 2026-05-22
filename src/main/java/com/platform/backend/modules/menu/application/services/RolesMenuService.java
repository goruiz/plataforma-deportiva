package com.platform.backend.modules.menu.application.services;

import com.platform.backend.modules.menu.application.iservices.IRolesMenuService;
import com.platform.backend.modules.menu.application.mappers.RolesMenuMapper;
import com.platform.backend.modules.menu.domain.entities.MenuEntity;
import com.platform.backend.modules.menu.domain.entities.RolesMenuEntity;
import com.platform.backend.modules.menu.domain.irepositories.IMenuRepository;
import com.platform.backend.modules.menu.domain.irepositories.IRolesMenuRepository;
import com.platform.backend.modules.menu.presentation.requests.CreateRolesMenuRequest.CreateRolesMenuRequest;
import com.platform.backend.modules.menu.presentation.requests.UpdateRolesMenuRequest.UpdateRolesMenuRequest;
import com.platform.backend.modules.menu.presentation.responses.RolesMenuResponse.RolesMenuResponse;
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
public class RolesMenuService implements IRolesMenuService {

    private final IRolesMenuRepository rolesMenuRepository;
    private final IRoleRepository roleRepository;
    private final IMenuRepository menuRepository;
    private final RolesMenuMapper rolesMenuMapper;

    @Override
    public List<RolesMenuResponse> getAll() {
        return rolesMenuRepository.findAllActive()
                .stream()
                .map(rolesMenuMapper::toResponse)
                .toList();
    }

    @Override
    public RolesMenuResponse getById(UUID id) {
        RolesMenuEntity rolesMenu = rolesMenuRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role-menu assignment not found with id: " + id));
        return rolesMenuMapper.toResponse(rolesMenu);
    }

    @Override
    public RolesMenuResponse create(CreateRolesMenuRequest request) {
        RolesEntity role = roleRepository.findActiveById(request.getRoleId())
                .orElseThrow(() -> new EntityNotFoundException("Role not found with id: " + request.getRoleId()));
        MenuEntity menu = menuRepository.findActiveById(request.getMenuId())
                .orElseThrow(() -> new EntityNotFoundException("Menu not found with id: " + request.getMenuId()));

        RolesMenuEntity rolesMenu = new RolesMenuEntity();
        rolesMenu.setRole(role);
        rolesMenu.setMenu(menu);
        rolesMenu.setVisible(request.getVisible());
        rolesMenu.setCanRead(request.getCanRead());
        rolesMenu.setCanCreat(request.getCanCreat());
        rolesMenu.setCanUpdate(request.getCanUpdate());
        rolesMenu.setCanDelete(request.getCanDelete());

        return rolesMenuMapper.toResponse(rolesMenuRepository.save(rolesMenu));
    }

    @Override
    public RolesMenuResponse update(UUID id, UpdateRolesMenuRequest request) {
        RolesMenuEntity rolesMenu = rolesMenuRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role-menu assignment not found with id: " + id));

        if (request.getVisible() != null) rolesMenu.setVisible(request.getVisible());
        if (request.getCanRead() != null) rolesMenu.setCanRead(request.getCanRead());
        if (request.getCanCreat() != null) rolesMenu.setCanCreat(request.getCanCreat());
        if (request.getCanUpdate() != null) rolesMenu.setCanUpdate(request.getCanUpdate());
        if (request.getCanDelete() != null) rolesMenu.setCanDelete(request.getCanDelete());

        return rolesMenuMapper.toResponse(rolesMenuRepository.save(rolesMenu));
    }

    @Override
    public void delete(UUID id) {
        RolesMenuEntity rolesMenu = rolesMenuRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role-menu assignment not found with id: " + id));
        rolesMenu.setDeletedAt(LocalDateTime.now());
        rolesMenuRepository.delete(rolesMenu);
    }

    @Override
    public void hardDelete(UUID id) {
        RolesMenuEntity rolesMenu = rolesMenuRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role-menu assignment not found with id: " + id));
        rolesMenuRepository.hardDelete(rolesMenu);
    }
}
