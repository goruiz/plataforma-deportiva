package com.platform.backend.modules.events.domain.entities;

import com.platform.backend.shared.domain.entities.BaseTranslatableEntity;
import com.platform.backend.shared.domain.interfaces.HasUniqueField;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "categories")
public class CategoriesEntity extends BaseTranslatableEntity implements HasUniqueField {

    private static final int NAME_MAX_LENGTH = 100;

    @Column(length = NAME_MAX_LENGTH, nullable = false)
    private String name;

    @Column(length = 255)
    private String description;

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
}
