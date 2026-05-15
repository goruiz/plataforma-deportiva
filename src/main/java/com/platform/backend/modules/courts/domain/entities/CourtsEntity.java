package com.platform.backend.modules.courts.domain.entities;

import com.platform.backend.shared.domain.entities.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "courts")
public class CourtsEntity extends BaseEntity {

    @Column(length = 150, nullable = false)
    private String name;

    @Column(length = 255)
    private String location;

    public CourtsEntity() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
