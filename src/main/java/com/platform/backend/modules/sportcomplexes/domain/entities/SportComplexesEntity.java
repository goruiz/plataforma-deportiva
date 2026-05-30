package com.platform.backend.modules.sportcomplexes.domain.entities;

import com.platform.backend.shared.domain.entities.BaseTranslatableEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "sport_complexes")
public class SportComplexesEntity extends BaseTranslatableEntity {

    @Column(length = 200, nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String location;

    public SportComplexesEntity() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
