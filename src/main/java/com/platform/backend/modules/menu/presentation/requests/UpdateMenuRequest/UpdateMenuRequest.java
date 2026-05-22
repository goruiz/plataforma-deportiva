package com.platform.backend.modules.menu.presentation.requests.UpdateMenuRequest;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateMenuRequest {

    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String description;

    @Size(max = 255)
    private String idParentMenu;

    private Integer order;

    @Size(max = 255)
    private String url;

    @Size(max = 255)
    private String icon;
}
