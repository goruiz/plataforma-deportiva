package com.platform.backend.modules.sportcomplexes.domain.entities;

import org.hibernate.annotations.Filter;
import com.platform.backend.shared.domain.tenancy.TenantFilters;
import com.platform.backend.shared.domain.tenancy.OrganizationOwned;
import com.platform.backend.shared.infraestructure.tenancy.OrganizationOwnerListener;
import com.platform.backend.shared.domain.entities.BaseTranslatableEntity;
import com.platform.backend.shared.domain.interfaces.HasUniqueField;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "sport_complexes")
@EntityListeners(OrganizationOwnerListener.class)
@Filter(name = TenantFilters.NAME, condition = TenantFilters.OWN_ORGANIZATION, deduceAliasInjectionPoints = false)
public class SportComplexesEntity extends BaseTranslatableEntity implements HasUniqueField, OrganizationOwned {

    @Column(length = 200, nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String location;

    // Exact position on the map; both are set or both are null (a CHECK in the database enforces it).
    private Double latitude;

    private Double longitude;

    /** The customer this record belongs to; null when it belongs to the platform owner. */
    @Column(name = "organization_id")
    private UUID organizationId;

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

    @Override
    public UUID getOrganizationId() { return organizationId; }

    @Override
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
}
