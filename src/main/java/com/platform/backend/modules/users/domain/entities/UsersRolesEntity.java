package com.platform.backend.modules.users.domain.entities;

import com.platform.backend.shared.domain.entities.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "users_roles")
public class UsersRolesEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user", foreignKey = @ForeignKey(name = "fk_users_roles_users_1"))
    private UsersEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_role", foreignKey = @ForeignKey(name = "fk_users_roles_roles_2"))
    private RolesEntity role;

    public UsersRolesEntity() {}

    public UsersEntity getUser() { return user; }
    public void setUser(UsersEntity user) { this.user = user; }

    public RolesEntity getRole() { return role; }
    public void setRole(RolesEntity role) { this.role = role; }
}
