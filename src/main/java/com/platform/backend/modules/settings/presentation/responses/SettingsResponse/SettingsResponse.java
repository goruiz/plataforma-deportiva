package com.platform.backend.modules.settings.presentation.responses.SettingsResponse;

import lombok.Data;

import java.util.UUID;

/** The match rules in force for the caller. */
@Data
public class SettingsResponse {

    private int matchWaitMinutes;
    private int maxSubstitutions;
    private boolean drawsAllowed;
    private int extraTimeMinutes;
    private boolean penaltiesEnabled;
    private int disputeWindowHours;

    /** The organization these rules belong to; null for the platform defaults. */
    private UUID organizationId;

    /** True when the caller's organization has no rules of its own and uses the platform defaults. */
    private boolean inheritedFromPlatform;
}
