package com.platform.backend.modules.matches.domain.entities;

import com.platform.backend.shared.domain.entities.BaseTranslatableEntity;
import com.platform.backend.shared.domain.interfaces.HasUniqueField;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "actions")
public class ActionsEntity extends BaseTranslatableEntity implements HasUniqueField {

    @Column(length = 255)
    private String name;

    @Column(length = 255)
    private String description;

    public ActionsEntity() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public void releaseUniqueFields(UUID entityId) {
        if (this.name != null) {
            this.name = this.name + "_DELETED_" + entityId;
        }
    }
}
