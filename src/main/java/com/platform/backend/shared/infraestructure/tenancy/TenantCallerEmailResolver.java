package com.platform.backend.shared.infraestructure.tenancy;

import java.util.function.Supplier;

/** Gives the filter its {@code callerEmail} parameter each time a query runs (see {@link CurrentTenant}). */
public class TenantCallerEmailResolver implements Supplier<String> {

    @Override
    public String get() {
        return CurrentTenant.callerEmail();
    }
}
