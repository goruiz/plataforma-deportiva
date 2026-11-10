package com.platform.backend.modules.settings.presentation.requests.UpdateSettingsRequest;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Replaces every rule at once: the ranges are checked against {@code app.settings.*} by the service. */
@Data
public class UpdateSettingsRequest {

    @NotNull
    private Integer matchWaitMinutes;

    @NotNull
    private Integer maxSubstitutions;

    @NotNull
    private Boolean drawsAllowed;

    @NotNull
    private Integer extraTimeMinutes;

    @NotNull
    private Boolean penaltiesEnabled;

    @NotNull
    private Integer disputeWindowHours;
}
