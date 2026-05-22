package com.platform.backend.modules.permissions.domain.entities;

import com.platform.backend.modules.users.domain.entities.RolesEntity;
import com.platform.backend.shared.domain.entities.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "permissions")
public class PermissionsEntity extends BaseEntity {

    @Column(length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_role", foreignKey = @ForeignKey(name = "fk_permissions_roles_1"))
    private RolesEntity role;

    public PermissionsEntity() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public RolesEntity getRole() { return role; }
    public void setRole(RolesEntity role) { this.role = role; }
}
