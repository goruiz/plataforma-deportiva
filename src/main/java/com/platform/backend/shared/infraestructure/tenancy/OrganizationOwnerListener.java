package com.platform.backend.shared.infraestructure.tenancy;

import com.platform.backend.shared.domain.tenancy.OrganizationOwned;
import com.platform.backend.shared.domain.tenancy.TenantScope;
import jakarta.persistence.PrePersist;

/**
 * Gives every new record the organization of whoever creates it. A caller restricted to an organization always
 * creates inside it, whatever the entity already says, so nobody can write into another customer's data. The platform
 * owner creates without an organization unless a service chose one on purpose.
 */
public class OrganizationOwnerListener {

    @PrePersist
    public void assignOrganization(Object entity) {
        if (!(entity instanceof OrganizationOwned owned)) {
            return;
        }
        TenantScope scope = CurrentTenant.scope();
        if (scope.restricted()) {
            owned.setOrganizationId(scope.organizationId());
        }
    }
}
