package com.platform.backend.modules.courts.domain.entities;

import org.hibernate.annotations.Filter;
import com.platform.backend.shared.domain.tenancy.TenantFilters;
import com.platform.backend.shared.domain.tenancy.OrganizationOwned;
import com.platform.backend.shared.infraestructure.tenancy.OrganizationOwnerListener;
import com.platform.backend.modules.sportcomplexes.domain.entities.SportComplexesEntity;
import com.platform.backend.shared.domain.entities.BaseTranslatableEntity;
import com.platform.backend.shared.domain.interfaces.HasUniqueField;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "courts")
@EntityListeners(OrganizationOwnerListener.class)
@Filter(name = TenantFilters.NAME, condition = TenantFilters.OWN_ORGANIZATION, deduceAliasInjectionPoints = false)
public class CourtsEntity extends BaseTranslatableEntity implements HasUniqueField, OrganizationOwned {

    @Column(length = 150, nullable = false)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(length = 255)
    private String location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sport_complex", foreignKey = @ForeignKey(name = "fk_courts_sport_complexes"))
    private SportComplexesEntity sportComplex;

    /** The customer this record belongs to; null when it belongs to the platform owner. */
    @Column(name = "organization_id")
    private UUID organizationId;

    public CourtsEntity() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public SportComplexesEntity getSportComplex() { return sportComplex; }
    public void setSportComplex(SportComplexesEntity sportComplex) { this.sportComplex = sportComplex; }

    @Override
    public void releaseUniqueFields(UUID entityId) {
        if (this.name != null) {
            this.name = this.name + "_DELETED_" + entityId;
        }
    }

    @Override
    public UUID getOrganizationId() { return organizationId; }

    @Override
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
}
