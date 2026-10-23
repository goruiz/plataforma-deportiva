package com.platform.backend.modules.menu.domain.exception;

/**
 * Why a request to create, change, move or delete a menu item was refused. The constant's name is the stable code
 * clients receive as the reason (in the field it concerns, or as the message when no single field is to blame), so they
 * can show it in their own language.
 */
public enum MenuError {

    /** The name is empty. */
    NAME_REQUIRED,

    /** Another menu item already has that name (any case). */
    NAME_ALREADY_EXISTS,

    /** The url is not a path inside the app: it must start with "/" and have no spaces. */
    INVALID_URL,

    /** Another menu item already opens that url. */
    URL_ALREADY_IN_USE,

    /** A group (an item with submenus) only holds other items, it opens nothing. */
    URL_NOT_ALLOWED_IN_GROUP,

    /** The parent does not exist (or was deleted). */
    PARENT_NOT_FOUND,

    /** The parent opens a screen (it has a url), so it cannot hold other items. */
    PARENT_HAS_URL,

    /** The item would end up inside itself or one of its own submenus. */
    PARENT_CREATES_LOOP,

    /** The item and what hangs from it would go deeper than the allowed number of levels. */
    MAX_DEPTH_EXCEEDED,

    /** An item cannot both go to the top level and into a parent. */
    PARENT_CONFLICT,

    /** The parent is exclusive to the owner, so what is inside it is too. */
    PARENT_OWNER_ONLY,

    /** The same item appears twice in the new order. */
    DUPLICATED_MENUS,

    /** A menu item the platform itself needs (the screens that manage the menu) cannot be deleted, hidden or changed this way. */
    SYSTEM_MENU,

    /** The item still has submenus: move or delete them first. */
    HAS_SUBMENUS
}
