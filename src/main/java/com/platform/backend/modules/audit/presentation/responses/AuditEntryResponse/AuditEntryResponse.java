package com.platform.backend.modules.audit.presentation.responses.AuditEntryResponse;

import com.platform.backend.modules.audit.domain.enums.AuditAction;
import com.platform.backend.modules.audit.domain.enums.AuditEntityType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/** One change in the audit trail: who created, changed or deleted which record, and when. */
@Data
public class AuditEntryResponse {
    private AuditEntityType entityType;
    private UUID entityId;
    private AuditAction action;
    private LocalDateTime occurredAt;
    private UUID actorId;
    /** Null when the author's account no longer exists. */
    private String actorName;
    /** The record's name. */
    private String label;
    /** Where the record belongs (its event, team or match), when it belongs somewhere. */
    private String context;
    /** A code that refines the type (the sort of match event), when there is one. */
    private String kind;
    /** The customer the record belongs to; null for what belongs to the platform owner. */
    private UUID organizationId;
    private String organizationName;
}
