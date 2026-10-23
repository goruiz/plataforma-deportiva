package com.platform.backend.modules.menu.domain.exception;

/**
 * Why a request about what a role sees in the menu was refused. The constant's name is the stable code clients
 * receive as the reason, so they can show it in their own language.
 */
public enum RolesMenuError {

    /** An owner-only screen cannot be given to any role but the platform owner's. */
    OWNER_ONLY_MENU,

    /** The platform owner's role cannot lose an owner-only screen: it would be left unable to get it back. */
    OWNER_MENU_REQUIRED
}
