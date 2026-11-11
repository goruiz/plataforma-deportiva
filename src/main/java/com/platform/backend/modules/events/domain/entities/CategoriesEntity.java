package com.platform.backend.modules.events.domain.entities;

import org.hibernate.annotations.Filter;
import com.platform.backend.modules.events.domain.models.CategoryRequirements;
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

    /** The gender the category admits ({@code Gender}); null admits both. */
    @Column(length = 10)
    private String gender;

    /** The youngest age admitted, in years completed; null has no minimum. */
    @Column(name = "min_age")
    private Integer minAge;

    /** The oldest age admitted, in years completed; null has no maximum. */
    @Column(name = "max_age")
    private Integer maxAge;

    public CategoriesEntity() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Integer getMinAge() { return minAge; }
    public void setMinAge(Integer minAge) { this.minAge = minAge; }

    public Integer getMaxAge() { return maxAge; }
    public void setMaxAge(Integer maxAge) { this.maxAge = maxAge; }

    /** What the category asks of whoever plays in it. */
    public CategoryRequirements requirements() {
        return new CategoryRequirements(gender, minAge, maxAge);
    }

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
