package com.platform.backend.modules.events.presentation.requests.UpdateCategoryRequest;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCategoryRequest {

    // Null means "leave unchanged"; a value, when sent, can't be blank.
    @Pattern(regexp = ".*\\S.*")
    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String description;

    // The rules of who may play in the category. Null leaves each one as it is; to take a rule away, send
    // gender ANY, or an age of 0.

    /** {@code MALE}, {@code FEMALE} or {@code ANY} (admits both). */
    @Size(max = 10)
    private String gender;

    /** The youngest age admitted, in years completed; 0 removes the minimum. */
    @Min(0)
    private Integer minAge;

    /** The oldest age admitted, in years completed; 0 removes the maximum. */
    @Min(0)
    private Integer maxAge;
}
