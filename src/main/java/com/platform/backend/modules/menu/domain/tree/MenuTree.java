package com.platform.backend.modules.menu.domain.tree;

import com.platform.backend.modules.menu.domain.entities.MenuEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The menu items as a tree, with the questions the rules about it need (what hangs from an item, how deep it is, who can
 * see it). It only reads the entities it is given — the active ones, in the order they should be shown — and never
 * changes them, so it knows nothing about persistence.
 * <p>
 * An item whose parent is not in the list (deleted, or never there) counts as a top-level one, like the menu the users
 * receive. A group is an item without a url: it only holds other items and opens nothing.
 */
public final class MenuTree {

    private final Map<UUID, MenuEntity> byId = new LinkedHashMap<>();
    /** The children of each item in display order; the key {@code null} holds the top-level items. */
    private final Map<UUID, List<MenuEntity>> childrenByParentId = new HashMap<>();

    public MenuTree(List<MenuEntity> menus) {
        menus.forEach(menu -> byId.put(menu.getId(), menu));
        menus.forEach(menu -> childrenByParentId
                .computeIfAbsent(effectiveParentId(menu), parentId -> new ArrayList<>())
                .add(menu));
    }

    public Optional<MenuEntity> find(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    public boolean contains(UUID id) {
        return byId.containsKey(id);
    }

    /** The children of the item (the top-level items for {@code null}), in display order. */
    public List<MenuEntity> childrenOf(UUID parentId) {
        return childrenByParentId.getOrDefault(parentId, List.of());
    }

    public boolean hasChildren(UUID id) {
        return !childrenOf(id).isEmpty();
    }

    /** True when {@code candidateId} hangs, at any depth, from {@code ancestorId}. */
    public boolean isDescendantOf(UUID candidateId, UUID ancestorId) {
        Set<UUID> visited = new HashSet<>();
        UUID current = parentIdOf(candidateId);
        while (current != null && visited.add(current)) {
            if (current.equals(ancestorId)) {
                return true;
            }
            current = parentIdOf(current);
        }
        return false;
    }

    /** Everything that hangs from the item, at any depth (not the item itself). */
    public List<MenuEntity> descendantsOf(UUID id) {
        List<MenuEntity> descendants = new ArrayList<>();
        collectDescendants(id, descendants, new HashSet<>());
        return descendants;
    }

    /** The levels from the top down to the item: 1 for a top-level item. */
    public int depthOf(UUID id) {
        int depth = 0;
        Set<UUID> visited = new HashSet<>();
        UUID current = id;
        while (current != null && contains(current) && visited.add(current)) {
            depth++;
            current = parentIdOf(current);
        }
        return depth;
    }

    /** The levels from the item down to its deepest descendant, itself included: 1 for an item with nothing under it. */
    public int heightOf(UUID id) {
        return heightOf(id, new HashSet<>());
    }

    /** True when the item, or anything under it, is one the platform itself needs. */
    public boolean containsSystemItem(UUID id) {
        MenuEntity menu = byId.get(id);
        return menu != null && (menu.isSystem() || descendantsOf(id).stream().anyMatch(MenuEntity::isSystem));
    }

    /** Where an item added at the end of this parent's children goes. */
    public int nextOrderUnder(UUID parentId) {
        return childrenOf(parentId).stream()
                .map(MenuEntity::getOrder)
                .filter(order -> order != null)
                .max(Integer::compare)
                .map(highest -> highest + 1)
                .orElse(0);
    }

    public boolean isNameTaken(String name, UUID exceptId) {
        return byId.values().stream()
                .anyMatch(menu -> !menu.getId().equals(exceptId) && equalsIgnoringCase(menu.getName(), name));
    }

    public boolean isUrlTaken(String url, UUID exceptId) {
        return byId.values().stream()
                .anyMatch(menu -> !menu.getId().equals(exceptId) && equalsIgnoringCase(menu.getUrl(), url));
    }

    /** A hidden item hides everything under it too. */
    public boolean isEffectivelyActive(MenuEntity menu) {
        Set<UUID> visited = new HashSet<>();
        MenuEntity current = menu;
        while (current != null && visited.add(current.getId())) {
            if (Boolean.FALSE.equals(current.getIsActive())) {
                return false;
            }
            current = byId.get(parentIdOf(current.getId()));
        }
        return true;
    }

    /**
     * What a user whose roles were granted {@code grantedIds} sees: the granted items that are not hidden, plus the groups
     * that hold them (a group needs no grant of its own), minus any group left with nothing inside. Display order is kept.
     */
    public List<MenuEntity> visibleFor(Set<UUID> grantedIds) {
        Set<UUID> shownIds = new HashSet<>();
        byId.values().stream()
                .filter(menu -> grantedIds.contains(menu.getId()) && isEffectivelyActive(menu))
                .forEach(menu -> addWithAncestors(menu.getId(), shownIds));

        Set<UUID> keptIds = new HashSet<>();
        childrenOf(null).forEach(root -> keepIfUseful(root, shownIds, keptIds));
        return byId.values().stream().filter(menu -> keptIds.contains(menu.getId())).toList();
    }

    private void addWithAncestors(UUID id, Set<UUID> shownIds) {
        UUID current = id;
        // add() is false once the item was already there, and then so were all its ancestors.
        while (current != null && contains(current) && shownIds.add(current)) {
            current = parentIdOf(current);
        }
    }

    /** A group is only worth showing when something inside it is; a screen is shown when it was shown at all. */
    private boolean keepIfUseful(MenuEntity menu, Set<UUID> shownIds, Set<UUID> keptIds) {
        if (!shownIds.contains(menu.getId())) {
            return false;
        }
        boolean hasKeptChild = false;
        for (MenuEntity child : childrenOf(menu.getId())) {
            hasKeptChild |= keepIfUseful(child, shownIds, keptIds);
        }
        boolean isGroup = menu.getUrl() == null || menu.getUrl().isBlank();
        if (isGroup && !hasKeptChild) {
            return false;
        }
        keptIds.add(menu.getId());
        return true;
    }

    private void collectDescendants(UUID id, List<MenuEntity> into, Set<UUID> visited) {
        for (MenuEntity child : childrenOf(id)) {
            if (visited.add(child.getId())) {
                into.add(child);
                collectDescendants(child.getId(), into, visited);
            }
        }
    }

    private int heightOf(UUID id, Set<UUID> visited) {
        if (!visited.add(id)) {
            return 0;
        }
        int deepestChild = childrenOf(id).stream()
                .mapToInt(child -> heightOf(child.getId(), visited))
                .max()
                .orElse(0);
        return 1 + deepestChild;
    }

    /** The parent the item really has in this tree: null for a top-level item, or when its stored parent is not here. */
    public UUID parentIdOf(UUID id) {
        MenuEntity menu = byId.get(id);
        return menu == null ? null : effectiveParentId(menu);
    }

    private UUID effectiveParentId(MenuEntity menu) {
        UUID parentId = menu.getIdParentMenu();
        return parentId != null && byId.containsKey(parentId) ? parentId : null;
    }

    private static boolean equalsIgnoringCase(String first, String second) {
        return first != null && second != null && first.trim().equalsIgnoreCase(second.trim());
    }
}
