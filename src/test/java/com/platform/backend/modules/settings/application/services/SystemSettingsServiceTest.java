package com.platform.backend.modules.settings.application.services;

import com.platform.backend.modules.settings.application.mappers.SystemSettingsMapper;
import com.platform.backend.modules.settings.domain.entities.SystemSettingsEntity;
import com.platform.backend.modules.settings.domain.irepositories.ISystemSettingsRepository;
import com.platform.backend.modules.settings.domain.models.SettingsValues;
import com.platform.backend.modules.settings.presentation.requests.UpdateSettingsRequest.UpdateSettingsRequest;
import com.platform.backend.modules.settings.presentation.responses.SettingsResponse.SettingsResponse;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import com.platform.backend.shared.infraestructure.tenancy.TenantTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemSettingsServiceTest {

    private static final UUID ORGANIZATION_ID = UUID.randomUUID();

    @Mock private ISystemSettingsRepository settingsRepository;

    private final SystemSettingsMapper settingsMapper = Mappers.getMapper(SystemSettingsMapper.class);
    private final SettingsLimits settingsLimits = new SettingsLimits(60, 20, 60, 1, 720);

    private SystemSettingsService service() {
        return new SystemSettingsService(settingsRepository, settingsMapper, settingsLimits);
    }

    @AfterEach
    void clearSecurityContext() {
        TenantTestSupport.actAsNobody();
    }

    private static SystemSettingsEntity rowOf(UUID organizationId, boolean drawsAllowed, boolean penaltiesEnabled) {
        SystemSettingsEntity row = new SystemSettingsEntity();
        row.setOrganizationId(organizationId);
        row.setMatchWaitMinutes(15);
        row.setMaxSubstitutions(5);
        row.setDrawsAllowed(drawsAllowed);
        row.setExtraTimeMinutes(0);
        row.setPenaltiesEnabled(penaltiesEnabled);
        row.setDisputeWindowHours(48);
        return row;
    }

    private static UpdateSettingsRequest request(boolean drawsAllowed, boolean penaltiesEnabled) {
        UpdateSettingsRequest request = new UpdateSettingsRequest();
        request.setMatchWaitMinutes(10);
        request.setMaxSubstitutions(7);
        request.setDrawsAllowed(drawsAllowed);
        request.setExtraTimeMinutes(20);
        request.setPenaltiesEnabled(penaltiesEnabled);
        request.setDisputeWindowHours(72);
        return request;
    }

    // ─── reading ─────────────────────────────────────────────────────────────────

    @Test
    void anOrganizationWithItsOwnRowReadsIt() {
        TenantTestSupport.actAsOrganizationAdmin(ORGANIZATION_ID);
        when(settingsRepository.findOfOrganization(ORGANIZATION_ID)).thenReturn(Optional.of(rowOf(ORGANIZATION_ID, false, true)));

        SettingsResponse response = service().getForCaller();

        assertThat(response.isDrawsAllowed()).isFalse();
        assertThat(response.isPenaltiesEnabled()).isTrue();
        assertThat(response.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
        assertThat(response.isInheritedFromPlatform()).isFalse();
    }

    @Test
    void anOrganizationWithoutARowInheritsThePlatformDefaultsAndIsToldSo() {
        TenantTestSupport.actAsOrganizationAdmin(ORGANIZATION_ID);
        when(settingsRepository.findOfOrganization(ORGANIZATION_ID)).thenReturn(Optional.empty());
        when(settingsRepository.findOfOrganization(null)).thenReturn(Optional.of(rowOf(null, true, false)));

        SettingsResponse response = service().getForCaller();

        assertThat(response.isDrawsAllowed()).isTrue();
        assertThat(response.getOrganizationId()).isNull();
        assertThat(response.isInheritedFromPlatform()).isTrue();
    }

    @Test
    void thePlatformOwnerReadsThePlatformDefaultsAsTheirOwn() {
        TenantTestSupport.actAsPlatformOwner();
        when(settingsRepository.findOfOrganization(null)).thenReturn(Optional.of(rowOf(null, true, false)));

        assertThat(service().getForCaller().isInheritedFromPlatform()).isFalse();
    }

    @Test
    void theCodeThatAppliesTheRulesFallsBackToThePlatformRow() {
        when(settingsRepository.findOfOrganization(ORGANIZATION_ID)).thenReturn(Optional.empty());
        when(settingsRepository.findOfOrganization(null)).thenReturn(Optional.of(rowOf(null, false, false)));

        SettingsValues values = service().effectiveFor(ORGANIZATION_ID);

        assertThat(values.drawsAllowed()).isFalse();
        assertThat(values.disputeWindowHours()).isEqualTo(48);
    }

    // ─── writing ─────────────────────────────────────────────────────────────────

    @Test
    void anAdministratorSavingForTheFirstTimeCreatesTheRowOfTheirOrganization() {
        TenantTestSupport.actAsOrganizationAdmin(ORGANIZATION_ID);
        when(settingsRepository.findOfOrganization(ORGANIZATION_ID)).thenReturn(Optional.empty());
        when(settingsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SettingsResponse response = service().update(request(false, true));

        ArgumentCaptor<SystemSettingsEntity> saved = ArgumentCaptor.forClass(SystemSettingsEntity.class);
        verify(settingsRepository).save(saved.capture());
        assertThat(saved.getValue().getOrganizationId()).isEqualTo(ORGANIZATION_ID);
        assertThat(saved.getValue().getMatchWaitMinutes()).isEqualTo(10);
        assertThat(saved.getValue().isDrawsAllowed()).isFalse();
        assertThat(saved.getValue().getDisputeWindowHours()).isEqualTo(72);
        assertThat(response.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
    }

    @Test
    void anAdministratorSavingAgainChangesTheirExistingRow() {
        TenantTestSupport.actAsOrganizationAdmin(ORGANIZATION_ID);
        SystemSettingsEntity existing = rowOf(ORGANIZATION_ID, true, false);
        when(settingsRepository.findOfOrganization(ORGANIZATION_ID)).thenReturn(Optional.of(existing));
        when(settingsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service().update(request(false, true));

        assertThat(existing.isDrawsAllowed()).isFalse();
        assertThat(existing.isPenaltiesEnabled()).isTrue();
        assertThat(existing.getExtraTimeMinutes()).isEqualTo(20);
    }

    @Test
    void thePlatformOwnerWritesThePlatformDefaultsNotAnOrganizationsRow() {
        TenantTestSupport.actAsPlatformOwner();
        SystemSettingsEntity platform = rowOf(null, true, false);
        when(settingsRepository.findOfOrganization(null)).thenReturn(Optional.of(platform));
        when(settingsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service().update(request(false, false));

        assertThat(platform.getOrganizationId()).isNull();
        assertThat(platform.isDrawsAllowed()).isFalse();
    }

    @Test
    void aNumberOutOfRangeIsRefusedWithACodeForEachOffendingField() {
        TenantTestSupport.actAsOrganizationAdmin(ORGANIZATION_ID);
        UpdateSettingsRequest tooBig = request(true, false);
        tooBig.setMatchWaitMinutes(61);
        tooBig.setDisputeWindowHours(0);

        assertThatThrownBy(() -> service().update(tooBig))
                .isInstanceOfSatisfying(FieldValidationException.class, refused ->
                        assertThat(refused.getFieldErrors())
                                .containsEntry("matchWaitMinutes", "OUT_OF_RANGE")
                                .containsEntry("disputeWindowHours", "OUT_OF_RANGE")
                                .doesNotContainKey("maxSubstitutions"));
        verify(settingsRepository, never()).save(any());
    }

    @Test
    void theLimitsThemselvesAreAllowed() {
        TenantTestSupport.actAsOrganizationAdmin(ORGANIZATION_ID);
        when(settingsRepository.findOfOrganization(ORGANIZATION_ID)).thenReturn(Optional.empty());
        when(settingsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        UpdateSettingsRequest edges = request(true, true);
        edges.setMatchWaitMinutes(0);
        edges.setMaxSubstitutions(20);
        edges.setExtraTimeMinutes(60);
        edges.setDisputeWindowHours(720);

        service().update(edges);

        verify(settingsRepository).save(any());
    }

    @Test
    void anAdministratorWithoutAnOrganizationHasNoRowToChange() {
        TenantTestSupport.actAsMobileAccountWithoutOrganization();

        assertThatThrownBy(() -> service().update(request(true, false))).isInstanceOf(AccessDeniedException.class);
        verify(settingsRepository, never()).save(any());
    }
}
