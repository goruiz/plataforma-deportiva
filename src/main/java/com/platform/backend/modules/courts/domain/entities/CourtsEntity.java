package com.platform.backend.modules.courts.domain.entities;

import com.platform.backend.modules.sportcomplexes.domain.entities.SportComplexesEntity;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sport_complex", foreignKey = @ForeignKey(name = "fk_courts_sport_complexes"))
    private SportComplexesEntity sportComplex;

    public CourtsEntity() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public SportComplexesEntity getSportComplex() { return sportComplex; }
    public void setSportComplex(SportComplexesEntity sportComplex) { this.sportComplex = sportComplex; }
}
