package com.platform.backend.shared.infraestructure.tenancy;

import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Checks for the ids a request carries as plain columns (a match's event, teams and court), which no foreign key or
 * lookup would otherwise validate. Without it, an administrator could point their records at another organization's.
 * <p>
 * Only callers restricted to one organization are checked: what they can look up is exactly what is theirs. Callers
 * that see everything keep the behavior they had.
 */
public final class TenantReferences {

    private TenantReferences() {}

    /**
     * Refuses an id the restricted caller cannot see. A missing id counts as not visible only when
     * {@code required} is true.
     *
     * @param isVisible looks the id up with the repository (whose queries already hide other organizations' rows)
     */
    public static void requireVisible(UUID id, boolean required, Predicate<UUID> isVisible,
                                      Supplier<? extends RuntimeException> otherwise) {
        if (CurrentTenant.scope().isUnrestricted()) {
            return;
        }
        boolean missingButRequired = id == null && required;
        boolean notVisible = id != null && !isVisible.test(id);
        if (missingButRequired || notVisible) {
            throw otherwise.get();
        }
    }
}
