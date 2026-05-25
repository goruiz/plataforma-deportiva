package com.platform.backend.modules.menu.presentation.requests.UpdateMenuRequest;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateMenuRequest {

    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String description;

    private UUID idParentMenu;

    private Integer order;

    @Size(max = 255)
    private String url;

    @Size(max = 255)
    private String icon;

    private Boolean isActive;

    private Short navOrder;

    @Size(max = 255)
    private String translationKey;
}
