package com.platform.backend.modules.menu.application.services;

import com.platform.backend.modules.menu.application.iservices.IMenuService;
import com.platform.backend.modules.menu.application.mappers.MenuMapper;
import com.platform.backend.modules.menu.domain.entities.MenuEntity;
import com.platform.backend.modules.menu.domain.entities.RolesMenuEntity;
import com.platform.backend.modules.menu.domain.exception.MenuError;
import com.platform.backend.modules.menu.domain.irepositories.IMenuRepository;
import com.platform.backend.modules.menu.domain.irepositories.IRolesMenuRepository;
import com.platform.backend.modules.menu.domain.tree.MenuTree;
import com.platform.backend.modules.menu.presentation.requests.CreateMenuRequest.CreateMenuRequest;
import com.platform.backend.modules.menu.presentation.requests.ReorderMenusRequest.ReorderMenusRequest;
import com.platform.backend.modules.menu.presentation.requests.UpdateMenuRequest.UpdateMenuRequest;
import com.platform.backend.modules.menu.presentation.responses.MenuResponse.MenuResponse;
import com.platform.backend.modules.menu.presentation.responses.MenuResponse.MenuTreeResponse;
import com.platform.backend.modules.users.domain.entities.RolesEntity;
import com.platform.backend.modules.users.domain.irepositories.IRoleRepository;
import com.platform.backend.modules.users.domain.irepositories.IUsersRolesRepository;
import com.platform.backend.modules.users.infrastructure.configuration.security.PlatformOwnerRole;
import com.platform.backend.shared.domain.utils.SoftDeleteHelper;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class MenuService implements IMenuService {

    private static final String NAME_FIELD = "name";
    private static final String URL_FIELD = "url";
    private static final String PARENT_FIELD = "idParentMenu";
    private static final String ACTIVE_FIELD = "isActive";
    private static final String OWNER_ONLY_FIELD = "ownerOnly";
    private static final String MENU_IDS_FIELD = "menuIds";

    /** A path inside the app: it starts with a single "/" and has no spaces ("//host" would leave the app). */
    private static final Pattern APP_PATH = Pattern.compile("^/(?!/)\\S*$");

    private final IMenuRepository menuRepository;
    private final IRolesMenuRepository rolesMenuRepository;
    private final IUsersRolesRepository usersRolesRepository;
    private final IRoleRepository roleRepository;
    private final PlatformOwnerRole platformOwnerRole;
    private final MenuMapper menuMapper;
    private final int maxDepth;

    public MenuService(IMenuRepository menuRepository,
                       IRolesMenuRepository rolesMenuRepository,
                       IUsersRolesRepository usersRolesRepository,
                       IRoleRepository roleRepository,
                       PlatformOwnerRole platformOwnerRole,
                       MenuMapper menuMapper,
                       @Value("${app.menu.max-depth:5}") int maxDepth) {
        this.menuRepository = menuRepository;
        this.rolesMenuRepository = rolesMenuRepository;
        this.usersRolesRepository = usersRolesRepository;
        this.roleRepository = roleRepository;
        this.platformOwnerRole = platformOwnerRole;
        this.menuMapper = menuMapper;
        this.maxDepth = maxDepth;
    }

    /** Every item, hidden ones included: this is the menu as it is managed, not as a user sees it. */
    @Override
    public List<MenuTreeResponse> getAll() {
        return buildTree(menuRepository.findAllActive());
    }

    /**
     * Regular roles see what was granted as visible in roles_menu. The platform owner sees every active menu item.
     * Groups need no grant of their own: they show while something inside them does
     * (see {@link MenuTree#visibleFor}). A regular user with no visible grants sees an empty menu.
     */
    @Override
    public List<MenuTreeResponse> getForUser(UUID userId) {
        List<UUID> roleIds = usersRolesRepository.findActiveRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }

        boolean isPlatformOwner = roleIds.stream().anyMatch(platformOwnerRole::isOwnerRole);
        Set<UUID> grantedMenuIds = rolesMenuRepository.findActiveVisibleByRoleIds(roleIds).stream()
                .map(RolesMenuEntity::getMenu)
                .map(MenuEntity::getId)
                .collect(Collectors.toSet());
        if (grantedMenuIds.isEmpty() && !isPlatformOwner) {
            return List.of();
        }

        List<MenuEntity> activeMenus = menuRepository.findAllActive();
        if (isPlatformOwner) {
            activeMenus.stream().map(MenuEntity::getId).forEach(grantedMenuIds::add);
        }
        return buildTree(new MenuTree(activeMenus).visibleFor(grantedMenuIds));
    }

    /**
     * Shared by getAll(), getForUser() and reorder(): they all turn a flat list into the same tree, only the input
     * list differs.
     */
    private List<MenuTreeResponse> buildTree(List<MenuEntity> entities) {
        Map<String, MenuTreeResponse> map = new LinkedHashMap<>();
        for (MenuEntity entity : entities) {
            MenuTreeResponse node = menuMapper.toTreeResponse(entity);
            node.setSubmenus(new ArrayList<>());
            map.put(entity.getId().toString(), node);
        }

        List<MenuTreeResponse> roots = new ArrayList<>();
        for (MenuEntity entity : entities) {
            MenuTreeResponse node = map.get(entity.getId().toString());
            UUID parentId = entity.getIdParentMenu();
            if (parentId == null) {
                roots.add(node);
            } else {
                MenuTreeResponse parent = map.get(parentId.toString());
                if (parent != null) {
                    parent.getSubmenus().add(node);
                } else {
                    roots.add(node);
                }
            }
        }

        return roots;
    }

    @Override
    public MenuResponse getById(UUID id) {
        MenuEntity menu = menuRepository.findActiveById(id)
                .orElseThrow(() -> menuNotFound(id));
        return menuMapper.toResponse(menu);
    }

    @Override
    @Transactional
    public MenuResponse create(CreateMenuRequest request) {
        MenuTree tree = loadTree();
        String name = requireFreeName(tree, request.getName(), null);
        String url = normalizeFreeUrl(tree, request.getUrl(), null);
        MenuEntity parent = findParent(tree, request.getIdParentMenu());
        requirePlacementAllowed(tree, parent, null);

        MenuEntity menu = menuMapper.toEntity(request);
        menu.setName(name);
        menu.setUrl(url);
        menu.setDescription(blankToNull(request.getDescription()));
        menu.setIcon(blankToNull(request.getIcon()));
        menu.setTranslationKey(blankToNull(request.getTranslationKey()));
        menu.setDescriptionTranslationKey(blankToNull(request.getDescriptionTranslationKey()));
        menu.setIdParentMenu(parent == null ? null : parent.getId());
        menu.setIsActive(request.getIsActive() == null || request.getIsActive());
        menu.setOrder(request.getOrder() != null ? request.getOrder() : tree.nextOrderUnder(menu.getIdParentMenu()));
        menu.setOwnerOnly(Boolean.TRUE.equals(request.getOwnerOnly()) || (parent != null && parent.isOwnerOnly()));

        MenuEntity saved = menuRepository.save(menu);
        // Nobody else sees it until «Menú por rol» says so, but the owner who made it can use it right away.
        if (opensAScreen(saved) || saved.isOwnerOnly()) {
            grantToOwnerRole(List.of(saved));
        }
        return menuMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public MenuResponse update(UUID id, UpdateMenuRequest request) {
        MenuTree tree = loadTree();
        MenuEntity menu = tree.find(id).orElseThrow(() -> menuNotFound(id));
        boolean wasOwnerOnly = menu.isOwnerOnly();

        String name = request.getName() == null ? null : requireFreeName(tree, request.getName(), id);
        String url = request.getUrl() == null ? null : validateUrlChange(tree, menu, request.getUrl());
        validateHidingAndOwnership(tree, menu, request);
        UUID newParentId = resolveNewParentId(tree, menu, request);
        boolean parentChanges = !Objects.equals(newParentId, tree.parentIdOf(id));
        MenuEntity newParent = parentChanges ? findParent(tree, newParentId) : null;
        if (parentChanges) {
            requirePlacementAllowed(tree, newParent, menu);
        }

        menuMapper.updateEntity(menu, request);
        applyNormalizedTexts(menu, request, name, url);
        if (parentChanges) {
            menu.setIdParentMenu(newParentId);
            menu.setOrder(tree.nextOrderUnder(newParentId));
            if (newParent != null && newParent.isOwnerOnly()) {
                menu.setOwnerOnly(true);
            }
        }

        MenuEntity saved = menuRepository.save(menu);
        if (!wasOwnerOnly && saved.isOwnerOnly()) {
            restrictToOwner(tree, saved);
        }
        return menuMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public List<MenuTreeResponse> reorder(ReorderMenusRequest request) {
        MenuTree tree = loadTree();
        List<UUID> requestedIds = request.getMenuIds();
        if (new HashSet<>(requestedIds).size() != requestedIds.size()) {
            throw new FieldValidationException(MENU_IDS_FIELD, MenuError.DUPLICATED_MENUS.name());
        }

        List<MenuEntity> moved = requestedIds.stream()
                .map(menuId -> tree.find(menuId).orElseThrow(() -> menuNotFound(menuId)))
                .toList();
        MenuEntity parent = findParent(tree, request.getIdParentMenu());
        moved.forEach(menu -> requirePlacementAllowed(tree, parent, menu));

        UUID parentId = parent == null ? null : parent.getId();
        List<MenuEntity> orderedSiblings = listMovedFirstThenRemainingChildren(tree, parentId, moved);
        List<MenuEntity> madeOwnerOnly = new ArrayList<>();
        for (int position = 0; position < orderedSiblings.size(); position++) {
            MenuEntity menu = orderedSiblings.get(position);
            menu.setIdParentMenu(parentId);
            menu.setOrder(position);
            if (parent != null && parent.isOwnerOnly() && !menu.isOwnerOnly()) {
                menu.setOwnerOnly(true);
                madeOwnerOnly.add(menu);
            }
            menuRepository.save(menu);
        }
        madeOwnerOnly.forEach(menu -> restrictToOwner(tree, menu));

        return buildTree(menuRepository.findAllActive());
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        MenuTree tree = loadTree();
        MenuEntity menu = tree.find(id).orElseThrow(() -> menuNotFound(id));
        requireDeletable(tree, menu);

        removeAssignmentsOf(List.of(id));
        SoftDeleteHelper.prepareForDeletion(menu);
        menuRepository.delete(menu);
    }

    @Override
    @Transactional
    public void hardDelete(UUID id) {
        MenuEntity menu = menuRepository.findById(id)
                .orElseThrow(() -> menuNotFound(id));
        requireDeletable(loadTree(), menu);
        menuRepository.hardDelete(menu);
    }

    private MenuTree loadTree() {
        return new MenuTree(menuRepository.findAllActive());
    }

    // ---- Validation of what the owner sends ----------------------------------------------------------------------

    /** The name without surrounding spaces; it must be there and no other item may have it. */
    private String requireFreeName(MenuTree tree, String rawName, UUID menuId) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) {
            throw new FieldValidationException(NAME_FIELD, MenuError.NAME_REQUIRED.name());
        }
        if (tree.isNameTaken(name, menuId)) {
            throw new FieldValidationException(NAME_FIELD, MenuError.NAME_ALREADY_EXISTS.name());
        }
        return name;
    }

    /** Null for "no url"; otherwise a path inside the app that no other item opens. */
    private String normalizeFreeUrl(MenuTree tree, String rawUrl, UUID menuId) {
        String url = blankToNull(rawUrl);
        if (url == null) {
            return null;
        }
        if (!APP_PATH.matcher(url).matches()) {
            throw new FieldValidationException(URL_FIELD, MenuError.INVALID_URL.name());
        }
        if (tree.isUrlTaken(url, menuId)) {
            throw new FieldValidationException(URL_FIELD, MenuError.URL_ALREADY_IN_USE.name());
        }
        return url;
    }

    /** Checks a url sent in an update. Sending the url the item already has is not a change and needs no checks. */
    private String validateUrlChange(MenuTree tree, MenuEntity menu, String rawUrl) {
        String url = blankToNull(rawUrl);
        if (Objects.equals(url, menu.getUrl())) {
            return url;
        }
        if (menu.isSystem()) {
            throw new FieldValidationException(URL_FIELD, MenuError.SYSTEM_MENU.name());
        }
        if (url != null && tree.hasChildren(menu.getId())) {
            throw new FieldValidationException(URL_FIELD, MenuError.URL_NOT_ALLOWED_IN_GROUP.name());
        }
        return normalizeFreeUrl(tree, url, menu.getId());
    }

    /** What the platform needs cannot be hidden or opened to everyone, and what is inside an owner's group stays the owner's. */
    private void validateHidingAndOwnership(MenuTree tree, MenuEntity menu, UpdateMenuRequest request) {
        if (Boolean.FALSE.equals(request.getIsActive()) && tree.containsSystemItem(menu.getId())) {
            throw new FieldValidationException(ACTIVE_FIELD, MenuError.SYSTEM_MENU.name());
        }
        if (Boolean.FALSE.equals(request.getOwnerOnly())) {
            if (menu.isSystem()) {
                throw new FieldValidationException(OWNER_ONLY_FIELD, MenuError.SYSTEM_MENU.name());
            }
            UUID parentId = tree.parentIdOf(menu.getId());
            boolean parentIsOwnerOnly = parentId != null && tree.find(parentId).map(MenuEntity::isOwnerOnly).orElse(false);
            if (parentIsOwnerOnly) {
                throw new FieldValidationException(OWNER_ONLY_FIELD, MenuError.PARENT_OWNER_ONLY.name());
            }
        }
    }

    /** Where the item will hang: the parent it asked for, the top level, or (when it says nothing) where it already is. */
    private UUID resolveNewParentId(MenuTree tree, MenuEntity menu, UpdateMenuRequest request) {
        boolean toTopLevel = Boolean.TRUE.equals(request.getTopLevel());
        if (toTopLevel && request.getIdParentMenu() != null) {
            throw new FieldValidationException(PARENT_FIELD, MenuError.PARENT_CONFLICT.name());
        }
        if (toTopLevel) {
            return null;
        }
        return request.getIdParentMenu() != null ? request.getIdParentMenu() : tree.parentIdOf(menu.getId());
    }

    private MenuEntity findParent(MenuTree tree, UUID parentId) {
        if (parentId == null) {
            return null;
        }
        return tree.find(parentId)
                .orElseThrow(() -> new FieldValidationException(PARENT_FIELD, MenuError.PARENT_NOT_FOUND.name()));
    }

    /**
     * Whether {@code moved} (or a new item, when null) can go inside {@code parent} (null = top level): the parent has to
     * be a group, the item cannot end up inside itself, and the levels must stay within the limit.
     */
    private void requirePlacementAllowed(MenuTree tree, MenuEntity parent, MenuEntity moved) {
        if (parent == null) {
            return;
        }
        if (opensAScreen(parent)) {
            throw new FieldValidationException(PARENT_FIELD, MenuError.PARENT_HAS_URL.name());
        }
        if (moved != null && (parent.getId().equals(moved.getId()) || tree.isDescendantOf(parent.getId(), moved.getId()))) {
            throw new FieldValidationException(PARENT_FIELD, MenuError.PARENT_CREATES_LOOP.name());
        }
        boolean staysWhereItIs = moved != null && Objects.equals(tree.parentIdOf(moved.getId()), parent.getId());
        int levels = tree.depthOf(parent.getId()) + (moved == null ? 1 : tree.heightOf(moved.getId()));
        if (!staysWhereItIs && levels > maxDepth) {
            throw new FieldValidationException(PARENT_FIELD, MenuError.MAX_DEPTH_EXCEEDED.name());
        }
    }

    /** What cannot be deleted: the platform's own screens, and anything that still holds other items. */
    private void requireDeletable(MenuTree tree, MenuEntity menu) {
        if (menu.isSystem()) {
            throw new IllegalStateException(MenuError.SYSTEM_MENU.name());
        }
        if (tree.hasChildren(menu.getId())) {
            throw new IllegalStateException(MenuError.HAS_SUBMENUS.name());
        }
    }

    // ---- Applying changes ----------------------------------------------------------------------------------------

    /**
     * The mapper copies what was sent as it came; this puts the cleaned-up text in its place. A blank text clears the
     * field, because the mapper ignores nulls and so "empty" is the only way a client can say "remove it".
     */
    private void applyNormalizedTexts(MenuEntity menu, UpdateMenuRequest request, String name, String url) {
        if (name != null) {
            menu.setName(name);
        }
        if (request.getUrl() != null) {
            menu.setUrl(url);
        }
        if (request.getDescription() != null) {
            menu.setDescription(blankToNull(request.getDescription()));
        }
        if (request.getIcon() != null) {
            menu.setIcon(blankToNull(request.getIcon()));
        }
        if (request.getTranslationKey() != null) {
            menu.setTranslationKey(blankToNull(request.getTranslationKey()));
        }
        if (request.getDescriptionTranslationKey() != null) {
            menu.setDescriptionTranslationKey(blankToNull(request.getDescriptionTranslationKey()));
        }
    }

    /** The items being placed come first, in the order asked; the parent's other children follow, as they were. */
    private List<MenuEntity> listMovedFirstThenRemainingChildren(MenuTree tree, UUID parentId, List<MenuEntity> moved) {
        Set<UUID> movedIds = moved.stream().map(MenuEntity::getId).collect(Collectors.toSet());
        List<MenuEntity> ordered = new ArrayList<>(moved);
        tree.childrenOf(parentId).stream()
                .filter(sibling -> !movedIds.contains(sibling.getId()))
                .forEach(ordered::add);
        return ordered;
    }

    // ---- Who sees what -------------------------------------------------------------------------------------------

    /**
     * Makes the item, and everything inside it, exclusive to the owner: no other role keeps seeing it, and the owner role
     * is certain to have it (an owner-only item the owner cannot see could never be given back).
     */
    private void restrictToOwner(MenuTree tree, MenuEntity root) {
        List<MenuEntity> affected = new ArrayList<>(List.of(root));
        affected.addAll(tree.descendantsOf(root.getId()));
        affected.stream()
                .filter(menu -> !menu.isOwnerOnly())
                .forEach(menu -> {
                    menu.setOwnerOnly(true);
                    menuRepository.save(menu);
                });

        revokeFromNonOwnerRoles(affected.stream().map(MenuEntity::getId).toList());
        grantToOwnerRole(affected);
    }

    private void grantToOwnerRole(Collection<MenuEntity> menus) {
        UUID ownerRoleId = platformOwnerRole.id().orElse(null);
        RolesEntity ownerRole = ownerRoleId == null ? null : roleRepository.findActiveById(ownerRoleId).orElse(null);
        if (ownerRole == null) {
            return;
        }

        Map<UUID, RolesMenuEntity> assignmentsByMenuId = rolesMenuRepository.findActiveByRoleId(ownerRoleId).stream()
                .collect(Collectors.toMap(assignment -> assignment.getMenu().getId(), Function.identity(), (first, second) -> first));
        for (MenuEntity menu : menus) {
            RolesMenuEntity assignment = assignmentsByMenuId.get(menu.getId());
            if (assignment == null) {
                rolesMenuRepository.save(RolesMenuEntity.visibleFor(ownerRole, menu));
            } else if (!Boolean.TRUE.equals(assignment.getVisible())) {
                assignment.setVisible(true);
                rolesMenuRepository.save(assignment);
            }
        }
    }

    private void revokeFromNonOwnerRoles(Collection<UUID> menuIds) {
        for (RolesMenuEntity assignment : rolesMenuRepository.findActiveByMenuIds(menuIds)) {
            boolean isOwnerRole = platformOwnerRole.isOwnerRole(assignment.getRole().getId());
            if (!isOwnerRole && Boolean.TRUE.equals(assignment.getVisible())) {
                assignment.setVisible(false);
                rolesMenuRepository.save(assignment);
            }
        }
    }

    /** A deleted item leaves no assignment behind: a role must not "see" something that no longer exists. */
    private void removeAssignmentsOf(Collection<UUID> menuIds) {
        for (RolesMenuEntity assignment : rolesMenuRepository.findActiveByMenuIds(menuIds)) {
            assignment.setDeletedAt(LocalDateTime.now());
            rolesMenuRepository.delete(assignment);
        }
    }

    // ---- Small helpers -------------------------------------------------------------------------------------------

    /** An item with a url opens a screen; one without is a group that only holds other items. */
    private boolean opensAScreen(MenuEntity menu) {
        return StringUtils.hasText(menu.getUrl());
    }

    private String blankToNull(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private EntityNotFoundException menuNotFound(UUID id) {
        return new EntityNotFoundException("Menu not found with id: " + id);
    }
}
