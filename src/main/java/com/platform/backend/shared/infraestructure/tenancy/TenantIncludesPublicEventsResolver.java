package com.platform.backend.shared.infraestructure.tenancy;

import java.util.function.Supplier;

/** Gives the filter its {@code includePublicEvents} parameter each time a query runs (see {@link CurrentTenant}). */
public class TenantIncludesPublicEventsResolver implements Supplier<Boolean> {

    @Override
    public Boolean get() {
        return CurrentTenant.scope().seesPublicEvents();
    }
}
