package com.platform.backend.shared.domain.tenancy;

import java.util.Objects;
import java.util.UUID;

/**
 * What the current caller may see: everything, or only what belongs to one organization.
 *
 * @param restricted     true when the caller only sees the rows of {@code organizationId}
 * @param organizationId the organization the caller is restricted to; null when not restricted
 */
public record TenantScope(boolean restricted, UUID organizationId) {

    /**
     * Value bound to the filter's organization parameter when nothing is restricted: it matches no organization and
     * keeps the parameter from ever being null.
     */
    public static final UUID NO_ORGANIZATION = new UUID(0L, 0L);

    public static TenantScope unrestricted() {
        return new TenantScope(false, null);
    }

    public static TenantScope ofOrganization(UUID organizationId) {
        return new TenantScope(true, Objects.requireNonNull(organizationId, "organizationId"));
    }

    public boolean isUnrestricted() {
        return !restricted;
    }

    /** The organization id the filter compares against. */
    public UUID organizationIdForFilter() {
        return restricted ? organizationId : NO_ORGANIZATION;
    }
}
