package com.platform.backend.modules.menu.domain.entities;

import com.platform.backend.shared.domain.entities.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "menu")
public class MenuEntity extends BaseEntity {

    @Column(length = 255)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "id_parent_menu", length = 255)
    private String idParentMenu;

    @Column(name = "\"order\"")
    private Integer order;

    @Column(length = 255)
    private String url;

    @Column(length = 255)
    private String icon;

    public MenuEntity() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIdParentMenu() { return idParentMenu; }
    public void setIdParentMenu(String idParentMenu) { this.idParentMenu = idParentMenu; }

    public Integer getOrder() { return order; }
    public void setOrder(Integer order) { this.order = order; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
}
