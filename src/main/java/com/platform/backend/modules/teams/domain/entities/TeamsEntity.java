package com.platform.backend.modules.teams.domain.entities;

import org.hibernate.annotations.Filter;
import com.platform.backend.shared.domain.tenancy.TenantFilters;
import com.platform.backend.shared.domain.tenancy.OrganizationOwned;
import com.platform.backend.shared.infraestructure.tenancy.OrganizationOwnerListener;
import com.platform.backend.modules.teams.domain.enums.TeamStatus;
import com.platform.backend.shared.domain.entities.BaseEntity;
import com.platform.backend.shared.domain.interfaces.HasUniqueField;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
// The name is unique inside each organization (a unique index created by the migrations), not in the whole system.
@Table(name = "teams")
@EntityListeners(OrganizationOwnerListener.class)
@Filter(name = TenantFilters.NAME, condition = TenantFilters.OWN_OR_PUBLIC_OR_OF_CALLER_TEAM, deduceAliasInjectionPoints = false)
public class TeamsEntity extends BaseEntity implements HasUniqueField, OrganizationOwned {

    private static final int NAME_MAX_LENGTH = 150;

    @Column(length = NAME_MAX_LENGTH, nullable = false)
    private String name;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(length = 20, nullable = false)
    private String status = TeamStatus.ACTIVE.name();

    /** The customer this record belongs to; null when it belongs to the platform owner. */
    @Column(name = "organization_id")
    private UUID organizationId;

    public TeamsEntity() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    /**
     * Frees the name for a new team by tagging it with this team's id. The original name is cut
     * when needed so the tagged one still fits the column (a long name would otherwise fail the delete).
     */
    @Override
    public void releaseUniqueFields(UUID entityId) {
        if (this.name != null) {
            String deletedTag = "_DELETED_" + entityId;
            int keptNameLength = Math.min(this.name.length(), NAME_MAX_LENGTH - deletedTag.length());
            this.name = this.name.substring(0, keptNameLength) + deletedTag;
        }
    }

    @Override
    public UUID getOrganizationId() { return organizationId; }

    @Override
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
}
