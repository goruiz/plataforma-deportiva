package com.platform.backend.modules.courts.presentation.requests.CreateCourtRequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCourtRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    @Size(max = 255)
    private String location;
}
