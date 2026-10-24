package com.platform.backend.modules.menu.application.services;

import com.platform.backend.modules.menu.application.iservices.IRolesMenuService;
import com.platform.backend.modules.menu.application.mappers.RolesMenuMapper;
import com.platform.backend.modules.menu.domain.entities.MenuEntity;
import com.platform.backend.modules.menu.domain.entities.RolesMenuEntity;
import com.platform.backend.modules.menu.domain.exception.RolesMenuError;
import com.platform.backend.modules.menu.domain.irepositories.IMenuRepository;
import com.platform.backend.modules.menu.domain.irepositories.IRolesMenuRepository;
import com.platform.backend.modules.menu.presentation.requests.CreateRolesMenuRequest.CreateRolesMenuRequest;
import com.platform.backend.modules.menu.presentation.requests.SaveRoleMenusRequest.SaveRoleMenusRequest;
import com.platform.backend.modules.menu.presentation.requests.UpdateRolesMenuRequest.UpdateRolesMenuRequest;
import com.platform.backend.modules.menu.presentation.responses.RolesMenuResponse.RolesMenuResponse;
import com.platform.backend.modules.users.domain.entities.RolesEntity;
import com.platform.backend.modules.users.domain.irepositories.IRoleRepository;
import com.platform.backend.modules.users.infrastructure.configuration.security.PlatformOwnerRole;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RolesMenuService implements IRolesMenuService {

    private static final String VISIBLE_FIELD = "visible";
    private static final String MENU_IDS_FIELD = "menuIds";

    private final IRolesMenuRepository rolesMenuRepository;
    private final IRoleRepository roleRepository;
    private final IMenuRepository menuRepository;
    private final RolesMenuMapper rolesMenuMapper;
    private final PlatformOwnerRole platformOwnerRole;

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
        validateOwnerOnlyRule(role.getId(), menu, Boolean.TRUE.equals(request.getVisible()), VISIBLE_FIELD);

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
    @Transactional
    public RolesMenuResponse update(UUID id, UpdateRolesMenuRequest request) {
        RolesMenuEntity rolesMenu = rolesMenuRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role-menu assignment not found with id: " + id));
        if (request.getVisible() != null) {
            validateOwnerOnlyRule(rolesMenu.getRole().getId(), rolesMenu.getMenu(), request.getVisible(), VISIBLE_FIELD);
        }

        if (request.getVisible() != null) rolesMenu.setVisible(request.getVisible());
        if (request.getCanRead() != null) rolesMenu.setCanRead(request.getCanRead());
        if (request.getCanCreat() != null) rolesMenu.setCanCreat(request.getCanCreat());
        if (request.getCanUpdate() != null) rolesMenu.setCanUpdate(request.getCanUpdate());
        if (request.getCanDelete() != null) rolesMenu.setCanDelete(request.getCanDelete());

        return rolesMenuMapper.toResponse(rolesMenuRepository.save(rolesMenu));
    }

    @Override
    @Transactional
    public List<RolesMenuResponse> saveForRole(UUID roleId, SaveRoleMenusRequest request) {
        RolesEntity role = roleRepository.findActiveById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found with id: " + roleId));
        Map<UUID, MenuEntity> menusById = menuRepository.findAllActive().stream()
                .collect(Collectors.toMap(MenuEntity::getId, Function.identity()));

        Set<UUID> visibleMenuIds = resolveVisibleMenuIds(role.getId(), request.getMenuIds(), menusById);
        applyVisibility(role, visibleMenuIds, menusById);

        return rolesMenuRepository.findActiveByRoleId(roleId).stream()
                .map(rolesMenuMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        RolesMenuEntity rolesMenu = rolesMenuRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role-menu assignment not found with id: " + id));
        requireOwnerRoleKeepsItsScreen(rolesMenu);
        rolesMenu.setDeletedAt(LocalDateTime.now());
        rolesMenuRepository.delete(rolesMenu);
    }

    @Override
    @Transactional
    public void hardDelete(UUID id) {
        RolesMenuEntity rolesMenu = rolesMenuRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role-menu assignment not found with id: " + id));
        requireOwnerRoleKeepsItsScreen(rolesMenu);
        rolesMenuRepository.hardDelete(rolesMenu);
    }

    /**
     * The menu items the role will see: what was asked for, which must all exist. The owner role always gets every
     * active item; any other role asking for an owner-only item is refused.
     */
    private Set<UUID> resolveVisibleMenuIds(UUID roleId, List<UUID> requestedMenuIds, Map<UUID, MenuEntity> menusById) {
        Set<UUID> visibleMenuIds = new LinkedHashSet<>(requestedMenuIds);
        visibleMenuIds.stream()
                .filter(menuId -> !menusById.containsKey(menuId))
                .findFirst()
                .ifPresent(unknownId -> {
                    throw new EntityNotFoundException("Menu not found with id: " + unknownId);
                });

        if (platformOwnerRole.isOwnerRole(roleId)) {
            visibleMenuIds.addAll(menusById.keySet());
        } else if (visibleMenuIds.stream().map(menusById::get).anyMatch(MenuEntity::isOwnerOnly)) {
            throw new FieldValidationException(MENU_IDS_FIELD, RolesMenuError.OWNER_ONLY_MENU.name());
        }
        return visibleMenuIds;
    }

    /** Updates what already exists (only when it changes) and creates what is missing, so the role ends up seeing exactly that. */
    private void applyVisibility(RolesEntity role, Set<UUID> visibleMenuIds, Map<UUID, MenuEntity> menusById) {
        Set<UUID> alreadyAssignedMenuIds = new HashSet<>();
        for (RolesMenuEntity assignment : rolesMenuRepository.findActiveByRoleId(role.getId())) {
            UUID menuId = assignment.getMenu().getId();
            alreadyAssignedMenuIds.add(menuId);

            boolean shouldBeVisible = visibleMenuIds.contains(menuId);
            if (!Objects.equals(assignment.getVisible(), shouldBeVisible)) {
                assignment.setVisible(shouldBeVisible);
                rolesMenuRepository.save(assignment);
            }
        }

        visibleMenuIds.stream()
                .filter(menuId -> !alreadyAssignedMenuIds.contains(menuId))
                .forEach(menuId -> rolesMenuRepository.save(RolesMenuEntity.visibleFor(role, menusById.get(menuId))));
    }

    /**
     * The owner role cannot lose any menu item; no other role may see an owner-only item.
     */
    private void validateOwnerOnlyRule(UUID roleId, MenuEntity menu, boolean visible, String field) {
        boolean isOwnerRole = platformOwnerRole.isOwnerRole(roleId);
        if (isOwnerRole && !visible) {
            throw new FieldValidationException(field, RolesMenuError.OWNER_MENU_REQUIRED.name());
        }
        if (!isOwnerRole && menu.isOwnerOnly() && visible) {
            throw new FieldValidationException(field, RolesMenuError.OWNER_ONLY_MENU.name());
        }
    }

    private void requireOwnerRoleKeepsItsScreen(RolesMenuEntity rolesMenu) {
        if (platformOwnerRole.isOwnerRole(rolesMenu.getRole().getId())) {
            throw new IllegalStateException(RolesMenuError.OWNER_MENU_REQUIRED.name());
        }
    }
}
