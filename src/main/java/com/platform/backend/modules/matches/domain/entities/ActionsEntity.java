package com.platform.backend.modules.matches.domain.entities;

import com.platform.backend.shared.domain.entities.BaseTranslatableEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "actions")
public class ActionsEntity extends BaseTranslatableEntity {

    @Column(length = 255)
    private String name;

    @Column(length = 255)
    private String description;

    public ActionsEntity() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
