package com.platform.backend.shared.infraestructure.tenancy;

import java.util.UUID;
import java.util.function.Supplier;

/** Gives the filter its {@code organizationId} parameter each time a query runs (see {@link CurrentTenant}). */
public class TenantOrganizationIdResolver implements Supplier<UUID> {

    @Override
    public UUID get() {
        return CurrentTenant.scope().organizationIdForFilter();
    }
}
