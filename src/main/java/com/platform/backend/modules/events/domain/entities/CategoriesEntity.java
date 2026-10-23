package com.platform.backend.modules.events.domain.entities;

import org.hibernate.annotations.Filter;
import com.platform.backend.shared.domain.tenancy.TenantFilters;
import com.platform.backend.shared.domain.tenancy.OrganizationOwned;
import com.platform.backend.shared.infraestructure.tenancy.OrganizationOwnerListener;
import com.platform.backend.shared.domain.entities.BaseTranslatableEntity;
import com.platform.backend.shared.domain.interfaces.HasUniqueField;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "categories")
@EntityListeners(OrganizationOwnerListener.class)
@Filter(name = TenantFilters.NAME, condition = TenantFilters.OWN_ORGANIZATION, deduceAliasInjectionPoints = false)
public class CategoriesEntity extends BaseTranslatableEntity implements HasUniqueField, OrganizationOwned {

    private static final int NAME_MAX_LENGTH = 100;

    @Column(length = NAME_MAX_LENGTH, nullable = false)
    private String name;

    @Column(length = 255)
    private String description;

    /** The customer this record belongs to; null when it belongs to the platform owner. */
    @Column(name = "organization_id")
    private UUID organizationId;

    public CategoriesEntity() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    /**
     * Frees the name for a new category by tagging it with this category's id. The original name is
     * cut when needed so the tagged one still fits the column (a long name would otherwise fail the delete).
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
