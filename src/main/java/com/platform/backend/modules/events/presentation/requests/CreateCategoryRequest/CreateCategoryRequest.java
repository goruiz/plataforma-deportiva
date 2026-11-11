package com.platform.backend.modules.events.presentation.requests.CreateCategoryRequest;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCategoryRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String description;

    // The rules of who may play in the category. All optional: a category without them admits everyone.

    /** {@code MALE} or {@code FEMALE}; null (or {@code ANY}) admits both. */
    @Size(max = 10)
    private String gender;

    /** The youngest age admitted, in years completed; null or 0 has no minimum. */
    @Min(0)
    private Integer minAge;

    /** The oldest age admitted, in years completed; null or 0 has no maximum. */
    @Min(0)
    private Integer maxAge;
}
