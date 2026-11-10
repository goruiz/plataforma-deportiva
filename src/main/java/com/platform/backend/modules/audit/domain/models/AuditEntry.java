package com.platform.backend.modules.audit.domain.models;

import com.platform.backend.modules.audit.domain.enums.AuditAction;
import com.platform.backend.modules.audit.domain.enums.AuditEntityType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One line of the audit trail: somebody created, changed or deleted a record. Only changes with a known author are
 * listed, so what the platform did on its own (seeded data, a self-registered account) is not.
 *
 * @param entityId         the record that changed
 * @param actorName        who made the change (their name, or their email when they have none); null when the account
 *                         no longer exists
 * @param label            the record's name (a match is its two teams, a goal the player who scored it)
 * @param context          where the record belongs (the event of a match or an enrollment, the team of a member, the
 *                         match of a goal, the email of an account); null when it does not belong anywhere
 * @param kind             a code that refines the type (the sort of match event: GOAL, YELLOW_CARD…); null otherwise
 * @param organizationId   the customer the record belongs to; null for what belongs to the platform owner
 * @param organizationName that customer's name, null when there is none
 */
public record AuditEntry(
        AuditEntityType entityType,
        UUID entityId,
        AuditAction action,
        LocalDateTime occurredAt,
        UUID actorId,
        String actorName,
        String label,
        String context,
        String kind,
        UUID organizationId,
        String organizationName) {

    /** A deleted record's name carries a technical tag (see {@link DeletedNameTag}) that the trail does not show. */
    public AuditEntry {
        label = DeletedNameTag.strip(label);
        context = DeletedNameTag.strip(context);
    }
}
