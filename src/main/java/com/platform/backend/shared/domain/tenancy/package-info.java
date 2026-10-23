/**
 * Multi-organization support: every customer of the platform (an organization) only sees its own data.
 * The filter is defined once here; each entity applies it with the condition that fits it ({@link TenantFilters}).
 */
@FilterDef(
        name = TenantFilters.NAME,
        autoEnabled = true,
        parameters = {
                @ParamDef(name = TenantFilters.UNRESTRICTED_PARAM, type = Boolean.class,
                        resolver = TenantUnrestrictedResolver.class),
                @ParamDef(name = TenantFilters.ORGANIZATION_ID_PARAM, type = UUID.class,
                        resolver = TenantOrganizationIdResolver.class)
        }
)
package com.platform.backend.shared.domain.tenancy;

import com.platform.backend.shared.infraestructure.tenancy.TenantOrganizationIdResolver;
import com.platform.backend.shared.infraestructure.tenancy.TenantUnrestrictedResolver;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.util.UUID;
