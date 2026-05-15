package com.platform.backend.modules.teams.domain.entities;

import com.platform.backend.shared.domain.entities.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "teams", uniqueConstraints = {
        @UniqueConstraint(name = "teams_name_key", columnNames = "name")
})
public class TeamsEntity extends BaseEntity {

    @Column(length = 150, nullable = false)
    private String name;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(length = 20, nullable = false)
    private String status = "ACTIVE";

    public TeamsEntity() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
