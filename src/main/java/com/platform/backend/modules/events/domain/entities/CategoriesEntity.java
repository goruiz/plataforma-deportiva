package com.platform.backend.modules.events.domain.entities;

import com.platform.backend.shared.domain.entities.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class CategoriesEntity extends BaseEntity {

    @Column(length = 100, nullable = false)
    private String name;

    public CategoriesEntity() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
