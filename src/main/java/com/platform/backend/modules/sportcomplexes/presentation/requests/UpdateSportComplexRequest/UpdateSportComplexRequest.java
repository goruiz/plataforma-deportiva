package com.platform.backend.modules.sportcomplexes.presentation.requests.UpdateSportComplexRequest;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double latitude;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double longitude;
}
