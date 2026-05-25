package com.platform.backend.modules.users.domain.entities;

import com.platform.backend.shared.domain.entities.BaseTranslatableEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class RolesEntity extends BaseTranslatableEntity {

    @Column(length = 255)
    private String name;

    @Column(length = 255)
    private String description;

    public RolesEntity() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
