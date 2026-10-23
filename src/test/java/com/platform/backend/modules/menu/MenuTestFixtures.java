package com.platform.backend.modules.menu;

import com.platform.backend.modules.menu.domain.entities.MenuEntity;

import java.util.UUID;

/** Menu items for the tests, built with just what each one needs. */
public final class MenuTestFixtures {

    private MenuTestFixtures() {}

    /** A screen: it has a url and, for the tests, hangs from {@code parent} (null = top level) at {@code order}. */
    public static MenuEntity screen(String name, String url, MenuEntity parent, int order) {
        MenuEntity menu = item(name, parent, order);
        menu.setUrl(url);
        return menu;
    }

    /** A group: no url, it only holds other items. */
    public static MenuEntity group(String name, MenuEntity parent, int order) {
        return item(name, parent, order);
    }

    private static MenuEntity item(String name, MenuEntity parent, int order) {
        MenuEntity menu = new MenuEntity();
        menu.setId(UUID.randomUUID());
        menu.setName(name);
        menu.setIdParentMenu(parent == null ? null : parent.getId());
        menu.setOrder(order);
        menu.setIsActive(true);
        return menu;
    }
}
