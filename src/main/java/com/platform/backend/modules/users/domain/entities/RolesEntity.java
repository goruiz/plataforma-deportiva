package com.platform.backend.modules.users.domain.entities;

import com.platform.backend.shared.domain.entities.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class RolesEntity extends BaseEntity {

    @Column(length = 255)
    private String name;

    public RolesEntity() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
