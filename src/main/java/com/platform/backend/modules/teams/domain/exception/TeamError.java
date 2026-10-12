package com.platform.backend.modules.teams.domain.exception;

/**
 * Why a team request was refused. The constant's name is the stable code clients
 * receive as the reason, so they can show it in their own language.
 */
public enum TeamError {
    NAME_ALREADY_EXISTS,
    CATEGORY_NOT_FOUND,
    INVALID_STATUS
}
