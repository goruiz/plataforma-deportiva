package com.platform.backend.modules.menu.domain.entities;

import com.platform.backend.modules.users.domain.entities.RolesEntity;
import com.platform.backend.shared.domain.entities.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "roles_menu")
public class RolesMenuEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_role", foreignKey = @ForeignKey(name = "fk_roles_menu_roles_1"))
    private RolesEntity role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_menu", foreignKey = @ForeignKey(name = "fk_roles_menu_menu_2"))
    private MenuEntity menu;

    @Column
    private Boolean visible;

    @Column(name = "can_read")
    private Boolean canRead;

    @Column(name = "can_creat")
    private Boolean canCreat;

    @Column(name = "can_update")
    private Boolean canUpdate;

    @Column(name = "can_delete")
    private Boolean canDelete;

    public RolesMenuEntity() {}

    public RolesEntity getRole() { return role; }
    public void setRole(RolesEntity role) { this.role = role; }

    public MenuEntity getMenu() { return menu; }
    public void setMenu(MenuEntity menu) { this.menu = menu; }

    public Boolean getVisible() { return visible; }
    public void setVisible(Boolean visible) { this.visible = visible; }

    public Boolean getCanRead() { return canRead; }
    public void setCanRead(Boolean canRead) { this.canRead = canRead; }

    public Boolean getCanCreat() { return canCreat; }
    public void setCanCreat(Boolean canCreat) { this.canCreat = canCreat; }

    public Boolean getCanUpdate() { return canUpdate; }
    public void setCanUpdate(Boolean canUpdate) { this.canUpdate = canUpdate; }

    public Boolean getCanDelete() { return canDelete; }
    public void setCanDelete(Boolean canDelete) { this.canDelete = canDelete; }
}
