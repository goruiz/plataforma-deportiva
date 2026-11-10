package com.platform.backend.modules.settings.application.iservices;

import com.platform.backend.modules.settings.domain.models.SettingsValues;
import com.platform.backend.modules.settings.presentation.requests.UpdateSettingsRequest.UpdateSettingsRequest;
import com.platform.backend.modules.settings.presentation.responses.SettingsResponse.SettingsResponse;

import java.util.UUID;

public interface ISystemSettingsService {

    /** The rules in force for the caller's organization (the platform defaults when it has none of its own). */
    SettingsResponse getForCaller();

    /**
     * Replaces the caller's rules: an administrator writes their organization's row (created from the platform's the
     * first time), the platform owner the defaults every organization without a row inherits.
     */
    SettingsResponse update(UpdateSettingsRequest request);

    /** The rules in force for an organization (null: the platform's), for the code that applies them. */
    SettingsValues effectiveFor(UUID organizationId);
}
