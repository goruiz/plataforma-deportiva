package com.platform.backend.modules.sportcomplexes.domain.entities;

import com.platform.backend.shared.domain.entities.BaseTranslatableEntity;
import com.platform.backend.shared.domain.interfaces.HasUniqueField;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "sport_complexes")
public class SportComplexesEntity extends BaseTranslatableEntity implements HasUniqueField {

    @Column(length = 200, nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String location;

    // Exact position on the map; both are set or both are null (a CHECK in the database enforces it).
    private Double latitude;

    private Double longitude;

    public SportComplexesEntity() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    @Override
    public void releaseUniqueFields(UUID entityId) {
        if (this.name != null) {
            this.name = this.name + "_DELETED_" + entityId;
        }
    }
}
