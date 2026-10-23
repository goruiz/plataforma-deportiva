package com.platform.backend.modules.organizations.domain.entities;

import com.platform.backend.modules.organizations.domain.enums.OrganizationStatus;
import com.platform.backend.shared.domain.entities.BaseEntity;
import com.platform.backend.shared.domain.interfaces.HasUniqueField;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * A customer of the platform: whoever bought the system to run its own championships (a league, a stadium, a
 * complex). Everything it creates belongs to it and no other organization can see it.
 */
@Entity
@Table(name = "organizations")
public class OrganizationsEntity extends BaseEntity implements HasUniqueField {

    private static final int NAME_MAX_LENGTH = 150;

    @Column(length = NAME_MAX_LENGTH, nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 20, nullable = false)
    private String status = OrganizationStatus.ACTIVE.name();

    public OrganizationsEntity() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isSuspended() {
        return OrganizationStatus.SUSPENDED.name().equals(status);
    }

    /**
     * Frees the name for a new organization by tagging it with this organization's id. The original name is cut
     * when needed so the tagged one still fits the column.
     */
    @Override
    public void releaseUniqueFields(UUID entityId) {
        if (this.name != null) {
            String deletedTag = "_DELETED_" + entityId;
            int keptNameLength = Math.min(this.name.length(), NAME_MAX_LENGTH - deletedTag.length());
            this.name = this.name.substring(0, keptNameLength) + deletedTag;
        }
    }
}
