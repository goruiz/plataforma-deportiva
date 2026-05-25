package com.platform.backend.modules.menu.presentation.responses.MenuResponse;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MenuResponse {

    private UUID id;
    private String name;
    private String description;
    private UUID idParentMenu;
    private Integer order;
    private String url;
    private String icon;
    private Boolean isActive;
    private Short navOrder;
    private String translationKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
