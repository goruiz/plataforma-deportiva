package com.platform.backend.modules.courts.domain.entities;

import com.platform.backend.shared.domain.entities.BaseTranslatableEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "courts")
public class CourtsEntity extends BaseTranslatableEntity {

    @Column(length = 150, nullable = false)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(length = 255)
    private String location;

    public CourtsEntity() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
