package com.platform.backend.modules.settings.application.services;

import com.platform.backend.modules.settings.application.iservices.ISystemSettingsService;
import com.platform.backend.modules.settings.application.mappers.SystemSettingsMapper;
import com.platform.backend.modules.settings.domain.entities.SystemSettingsEntity;
import com.platform.backend.modules.settings.domain.irepositories.ISystemSettingsRepository;
import com.platform.backend.modules.settings.domain.models.SettingsValues;
import com.platform.backend.modules.settings.presentation.requests.UpdateSettingsRequest.UpdateSettingsRequest;
import com.platform.backend.modules.settings.presentation.responses.SettingsResponse.SettingsResponse;
import com.platform.backend.shared.domain.tenancy.TenantScope;
import com.platform.backend.shared.infraestructure.tenancy.CurrentTenant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SystemSettingsService implements ISystemSettingsService {

    private final ISystemSettingsRepository settingsRepository;
    private final SystemSettingsMapper settingsMapper;
    private final SettingsLimits settingsLimits;

    @Override
    public SettingsResponse getForCaller() {
        UUID organizationId = organizationOfCaller();
        return settingsRepository.findOfOrganization(organizationId)
                .map(settingsMapper::toResponse)
                .orElseGet(() -> inheritedFromPlatform());
    }

    @Override
    @Transactional
    public SettingsResponse update(UpdateSettingsRequest request) {
        settingsLimits.requireWithinLimits(request);
        UUID organizationId = organizationToWrite();
        SystemSettingsEntity settings = settingsRepository.findOfOrganization(organizationId)
                .orElseGet(() -> newRowFor(organizationId));
        apply(request, settings);
        return settingsMapper.toResponse(settingsRepository.save(settings));
    }

    @Override
    public SettingsValues effectiveFor(UUID organizationId) {
        return settingsRepository.findOfOrganization(organizationId)
                .or(() -> settingsRepository.findOfOrganization(null))
                .map(settingsMapper::toValues)
                .orElseThrow(() -> new IllegalStateException("The platform settings row is missing"));
    }

    /** The caller's organization; null for the platform owner and for an account that belongs to none. */
    private UUID organizationOfCaller() {
        TenantScope scope = CurrentTenant.scope();
        return scope.isUnrestricted() ? null : scope.organizationId();
    }

    /** An administrator writes their own organization's row; only the platform owner writes the platform's. */
    private UUID organizationToWrite() {
        if (CurrentTenant.isPlatformOwner()) {
            return null;
        }
        UUID organizationId = organizationOfCaller();
        if (organizationId == null) {
            throw new AccessDeniedException("An administrator without an organization has no settings to change");
        }
        return organizationId;
    }

    private SettingsResponse inheritedFromPlatform() {
        SystemSettingsEntity platform = settingsRepository.findOfOrganization(null)
                .orElseThrow(() -> new IllegalStateException("The platform settings row is missing"));
        SettingsResponse response = settingsMapper.toResponse(platform);
        response.setInheritedFromPlatform(organizationOfCaller() != null);
        return response;
    }

    /** The first time an organization saves it gets a row of its own; until then it uses the platform's. */
    private SystemSettingsEntity newRowFor(UUID organizationId) {
        SystemSettingsEntity row = new SystemSettingsEntity();
        row.setOrganizationId(organizationId);
        return row;
    }

    private void apply(UpdateSettingsRequest request, SystemSettingsEntity settings) {
        settings.setMatchWaitMinutes(request.getMatchWaitMinutes());
        settings.setMaxSubstitutions(request.getMaxSubstitutions());
        settings.setDrawsAllowed(request.getDrawsAllowed());
        settings.setExtraTimeMinutes(request.getExtraTimeMinutes());
        settings.setPenaltiesEnabled(request.getPenaltiesEnabled());
        settings.setDisputeWindowHours(request.getDisputeWindowHours());
    }
}
