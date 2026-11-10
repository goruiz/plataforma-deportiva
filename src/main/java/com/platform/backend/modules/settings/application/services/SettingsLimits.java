package com.platform.backend.modules.settings.application.services;

import com.platform.backend.modules.settings.domain.exception.SettingsError;
import com.platform.backend.modules.settings.presentation.requests.UpdateSettingsRequest.UpdateSettingsRequest;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What each number of the settings may be ({@code app.settings.*}): an unbounded value would let one request make a
 * dispute window last for years. The panel repeats these limits in {@code SETTINGS_LIMITS}.
 */
@Component
public class SettingsLimits {

    private final int maxMatchWaitMinutes;
    private final int maxSubstitutions;
    private final int maxExtraTimeMinutes;
    private final int minDisputeWindowHours;
    private final int maxDisputeWindowHours;

    public SettingsLimits(
            @Value("${app.settings.max-match-wait-minutes:60}") int maxMatchWaitMinutes,
            @Value("${app.settings.max-substitutions:20}") int maxSubstitutions,
            @Value("${app.settings.max-extra-time-minutes:60}") int maxExtraTimeMinutes,
            @Value("${app.settings.min-dispute-window-hours:1}") int minDisputeWindowHours,
            @Value("${app.settings.max-dispute-window-hours:720}") int maxDisputeWindowHours) {
        this.maxMatchWaitMinutes = maxMatchWaitMinutes;
        this.maxSubstitutions = maxSubstitutions;
        this.maxExtraTimeMinutes = maxExtraTimeMinutes;
        this.minDisputeWindowHours = minDisputeWindowHours;
        this.maxDisputeWindowHours = maxDisputeWindowHours;
    }

    /** Rejects every number out of range at once, so the client can mark all of them in one answer. */
    public void requireWithinLimits(UpdateSettingsRequest request) {
        Map<String, String> outOfRange = new LinkedHashMap<>();
        check(outOfRange, "matchWaitMinutes", request.getMatchWaitMinutes(), 0, maxMatchWaitMinutes);
        check(outOfRange, "maxSubstitutions", request.getMaxSubstitutions(), 0, maxSubstitutions);
        check(outOfRange, "extraTimeMinutes", request.getExtraTimeMinutes(), 0, maxExtraTimeMinutes);
        check(outOfRange, "disputeWindowHours", request.getDisputeWindowHours(), minDisputeWindowHours, maxDisputeWindowHours);
        if (!outOfRange.isEmpty()) {
            throw new FieldValidationException(outOfRange);
        }
    }

    private static void check(Map<String, String> outOfRange, String field, int value, int min, int max) {
        if (value < min || value > max) {
            outOfRange.put(field, SettingsError.OUT_OF_RANGE.name());
        }
    }
}
