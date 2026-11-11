package com.platform.backend.modules.audit.domain.filters;

import com.platform.backend.modules.audit.domain.enums.AuditAction;
import com.platform.backend.modules.audit.domain.enums.AuditEntityType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * What the audit trail is narrowed by. Null (or empty) means "no condition of that kind".
 *
 * @param searchTerms lower-cased words; each one must appear in the record's name, where it belongs, its kind or the
 *                    name of who made the change
 * @param entityType  only changes to this kind of record
 * @param action      only this kind of change
 * @param from        changes at or after this instant
 * @param until       changes before this instant (exclusive, so a whole last day is the start of the next one)
 * @param organizationId  only what belongs to this administrator's organization. Only whoever sees every organization (the
 *                    platform owner) can narrow by one: for an administrator of an organization the scope already is theirs
 * @param withoutOrganization only what belongs to no organization, that is, to the platform owner
 */
public record AuditFilter(
        List<String> searchTerms,
        AuditEntityType entityType,
        AuditAction action,
        LocalDateTime from,
        LocalDateTime until,
        UUID organizationId,
        boolean withoutOrganization) {

    /** A filter that does not narrow by organization. */
    public AuditFilter(List<String> searchTerms, AuditEntityType entityType, AuditAction action, LocalDateTime from,
                       LocalDateTime until) {
        this(searchTerms, entityType, action, from, until, null, false);
    }
}
