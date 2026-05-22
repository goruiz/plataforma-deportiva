package com.platform.backend.modules.matches.presentation.requests.CreateActionRequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateActionRequest {

    @NotBlank
    @Size(max = 255)
    private String name;
}
