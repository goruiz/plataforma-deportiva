package com.platform.backend.modules.sportcomplexes.presentation.requests.UpdateSportComplexRequest;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateSportComplexRequest {

    @Size(max = 200)
    private String name;

    @Size(max = 500)
    private String description;

    @Size(max = 500)
    private String location;
}
