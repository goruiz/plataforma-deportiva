package com.platform.backend.modules.audit.domain.exception;

/**
 * Why an audit request was refused. The constant's name is the stable code clients receive as the reason, so they can
 * show it in their own language.
 */
public enum AuditError {
    /** {@code entityType} is not one of the audited kinds of record. */
    INVALID_ENTITY_TYPE,
    /** {@code action} is not CREATED, UPDATED or DELETED. */
    INVALID_ACTION,
    /** A date is not written as yyyy-MM-dd. */
    INVALID_DATE,
    /** The last day comes before the first one. */
    DATE_RANGE_INVERTED
}
