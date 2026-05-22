package com.platform.backend.modules.events.presentation.requests.UpdateCategoryRequest;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCategoryRequest {

    @Size(max = 100)
    private String name;
}
