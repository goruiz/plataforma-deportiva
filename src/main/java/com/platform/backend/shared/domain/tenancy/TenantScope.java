package com.platform.backend.shared.domain.tenancy;

import java.util.Objects;
import java.util.UUID;

/**
 * What the current caller may see: everything, or only what belongs to one organization.
 *
 * @param restricted        true when the caller only sees the rows of {@code organizationId}
 * @param organizationId    the organization the caller is restricted to; null when not restricted and for a player who
 *                          belongs to none
 * @param seesPublicEvents  true for a player, who besides their organization sees the events marked as public
 */
public record TenantScope(boolean restricted, UUID organizationId, boolean seesPublicEvents) {

    /**
     * Value bound to the filter's organization parameter when no organization applies: it matches no organization and
     * keeps the parameter from ever being null.
     */
    public static final UUID NO_ORGANIZATION = new UUID(0L, 0L);

    public static TenantScope unrestricted() {
        return new TenantScope(false, null, false);
    }

    /** An administrator: only their organization, whatever other organizations publish. */
    public static TenantScope ofOrganization(UUID organizationId) {
        return new TenantScope(true, Objects.requireNonNull(organizationId, "organizationId"), false);
    }

    /** A player: their organization (none yet when they have no team) and the public events of everyone. */
    public static TenantScope ofPlayer(UUID organizationId) {
        return new TenantScope(true, organizationId, true);
    }

    public boolean isUnrestricted() {
        return !restricted;
    }

    /** The organization id the filter compares against. */
    public UUID organizationIdForFilter() {
        return restricted && organizationId != null ? organizationId : NO_ORGANIZATION;
    }
}
