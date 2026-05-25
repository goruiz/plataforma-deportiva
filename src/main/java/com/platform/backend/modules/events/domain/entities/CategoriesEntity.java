package com.platform.backend.modules.events.domain.entities;

import com.platform.backend.shared.domain.entities.BaseTranslatableEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class CategoriesEntity extends BaseTranslatableEntity {

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 255)
    private String description;

    public CategoriesEntity() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
