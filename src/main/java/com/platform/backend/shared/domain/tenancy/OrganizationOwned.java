package com.platform.backend.shared.domain.tenancy;

import java.util.UUID;

/**
 * Implemented by the entities that belong to one organization (a customer of the platform). A null organization
 * means the record is not owned by any customer: it belongs to the platform owner.
 * <p>
 * Reads are narrowed by the {@link TenantFilters} filter and the organization of a new record is set when it is
 * persisted (see {@code OrganizationOwnerListener}), so services never copy it by hand.
 */
public interface OrganizationOwned {

    UUID getOrganizationId();

    void setOrganizationId(UUID organizationId);
}
