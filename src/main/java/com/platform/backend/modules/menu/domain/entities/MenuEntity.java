package com.platform.backend.modules.menu.domain.entities;

import com.platform.backend.shared.domain.entities.BaseTranslatableEntity;
import com.platform.backend.shared.domain.interfaces.HasUniqueField;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "menu")
public class MenuEntity extends BaseTranslatableEntity implements HasUniqueField {

    @Column(length = 255)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "id_parent_menu")
    private UUID idParentMenu;

    @Column(name = "\"order\"")
    private Integer order;

    @Column(length = 255)
    private String url;

    @Column(length = 255)
    private String icon;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "nav_order")
    private Short navOrder;

    public MenuEntity() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public UUID getIdParentMenu() { return idParentMenu; }
    public void setIdParentMenu(UUID idParentMenu) { this.idParentMenu = idParentMenu; }

    public Integer getOrder() { return order; }
    public void setOrder(Integer order) { this.order = order; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Short getNavOrder() { return navOrder; }
    public void setNavOrder(Short navOrder) { this.navOrder = navOrder; }

    @Override
    public void releaseUniqueFields(UUID entityId) {
        if (this.name != null) {
            this.name = this.name + "_DELETED_" + entityId;
        }
    }
}
