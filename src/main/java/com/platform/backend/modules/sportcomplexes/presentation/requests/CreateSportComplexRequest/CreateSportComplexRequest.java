package com.platform.backend.modules.sportcomplexes.presentation.requests.CreateSportComplexRequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSportComplexRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 500)
    private String description;

    @Size(max = 500)
    private String location;
}
