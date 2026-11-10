package com.platform.backend.modules.audit.domain.enums;

/** What happened to a record. The constant's name is the stable code clients receive. */
public enum AuditAction {
    CREATED,
    /** The last change made to a record that still exists (earlier changes are not kept). */
    UPDATED,
    DELETED
}
