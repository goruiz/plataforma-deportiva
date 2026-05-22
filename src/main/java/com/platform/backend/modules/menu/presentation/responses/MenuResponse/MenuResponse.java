package com.platform.backend.modules.menu.presentation.responses.MenuResponse;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MenuResponse {

    private UUID id;
    private String name;
    private String description;
    private String idParentMenu;
    private Integer order;
    private String url;
    private String icon;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
