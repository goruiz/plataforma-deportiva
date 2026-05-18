package com.platform.backend.modules.events.presentation.requests.CreateCategoryRequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCategoryRequest {

    @NotBlank
    @Size(max = 100)
    private String name;
}
