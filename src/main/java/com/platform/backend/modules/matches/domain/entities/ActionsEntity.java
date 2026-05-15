package com.platform.backend.modules.matches.domain.entities;

import com.platform.backend.shared.domain.entities.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "actions")
public class ActionsEntity extends BaseEntity {

    @Column(length = 255)
    private String name;

    public ActionsEntity() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
