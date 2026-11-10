package com.platform.backend.modules.audit.domain.filters;

import com.platform.backend.modules.audit.domain.enums.AuditAction;
import com.platform.backend.modules.audit.domain.enums.AuditEntityType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * What the audit trail is narrowed by. Null (or empty) means "no condition of that kind".
 *
 * @param searchTerms lower-cased words; each one must appear in the record's name, where it belongs, its kind or the
 *                    name of who made the change
 * @param entityType  only changes to this kind of record
 * @param action      only this kind of change
 * @param from        changes at or after this instant
 * @param until       changes before this instant (exclusive, so a whole last day is the start of the next one)
 */
public record AuditFilter(
        List<String> searchTerms,
        AuditEntityType entityType,
        AuditAction action,
        LocalDateTime from,
        LocalDateTime until) {
}
