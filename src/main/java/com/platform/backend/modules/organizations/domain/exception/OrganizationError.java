package com.platform.backend.modules.organizations.domain.exception;

/**
 * Why an organization request was refused. The constant's name is the stable code clients
 * receive as the reason, so they can show it in their own language.
 */
public enum OrganizationError {
    NAME_ALREADY_EXISTS,
    INVALID_STATUS,
    IN_USE
}
