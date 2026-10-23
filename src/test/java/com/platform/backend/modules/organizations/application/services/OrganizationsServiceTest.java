package com.platform.backend.modules.organizations.application.services;

import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.organizations.application.mappers.OrganizationMapper;
import com.platform.backend.modules.organizations.domain.entities.OrganizationsEntity;
import com.platform.backend.modules.organizations.domain.enums.OrganizationStatus;
import com.platform.backend.modules.organizations.domain.irepositories.IOrganizationRepository;
import com.platform.backend.modules.organizations.presentation.requests.CreateOrganizationRequest.CreateOrganizationRequest;
import com.platform.backend.modules.organizations.presentation.requests.UpdateOrganizationRequest.UpdateOrganizationRequest;
import com.platform.backend.modules.organizations.presentation.responses.OrganizationResponse.OrganizationResponse;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationsServiceTest {

    @Mock private IOrganizationRepository organizationRepository;
    @Mock private IUserRepository userRepository;
    @Mock private IEventRepository eventRepository;
    @Mock private ITeamRepository teamRepository;
    @Spy private OrganizationMapper organizationMapper = Mappers.getMapper(OrganizationMapper.class);

    @InjectMocks
    private OrganizationsService organizationsService;

    private OrganizationsEntity organization;

    @BeforeEach
    void setUp() {
        organization = new OrganizationsEntity();
        organization.setId(UUID.randomUUID());
        organization.setName("Liga Norte");

        lenient().when(organizationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static CreateOrganizationRequest createRequest(String name, String description) {
        CreateOrganizationRequest request = new CreateOrganizationRequest();
        request.setName(name);
        request.setDescription(description);
        return request;
    }

    private void givenTheOrganizationExists() {
        when(organizationRepository.findActiveById(organization.getId())).thenReturn(Optional.of(organization));
    }

    // ─── create ─────────────────────────────────────────────────────────────────

    @Test
    void create_whenNameIsFree_thenSaveItTrimmedAndActive() {
        when(organizationRepository.existsByName("Liga Sur")).thenReturn(false);

        OrganizationResponse response = organizationsService.create(createRequest("  Liga Sur  ", "  Barrio Sur "));

        assertThat(response.getName()).isEqualTo("Liga Sur");
        assertThat(response.getDescription()).isEqualTo("Barrio Sur");
        assertThat(response.getStatus()).isEqualTo(OrganizationStatus.ACTIVE.name());
    }

    @Test
    void create_whenDescriptionIsBlank_thenItIsStoredAsNull() {
        OrganizationResponse response = organizationsService.create(createRequest("Liga Sur", "   "));

        assertThat(response.getDescription()).isNull();
    }

    @Test
    void create_whenNameAlreadyExists_thenRejectTheNameFieldWithoutSaving() {
        when(organizationRepository.existsByName("Liga Norte")).thenReturn(true);

        assertThatThrownBy(() -> organizationsService.create(createRequest("Liga Norte", null)))
                .isInstanceOfSatisfying(FieldValidationException.class, exception ->
                        assertThat(exception.getFieldErrors()).containsOnly(entry("name", "NAME_ALREADY_EXISTS")));

        verify(organizationRepository, never()).save(any());
    }

    // ─── update ─────────────────────────────────────────────────────────────────

    @Test
    void update_whenSuspending_thenTheStatusChanges() {
        givenTheOrganizationExists();
        UpdateOrganizationRequest request = new UpdateOrganizationRequest();
        request.setStatus(OrganizationStatus.SUSPENDED.name());

        OrganizationResponse response = organizationsService.update(organization.getId(), request);

        assertThat(response.getStatus()).isEqualTo(OrganizationStatus.SUSPENDED.name());
        assertThat(organization.isSuspended()).isTrue();
    }

    @Test
    void update_whenStatusIsNotOneOfTheKnownOnes_thenRejectTheStatusField() {
        givenTheOrganizationExists();
        UpdateOrganizationRequest request = new UpdateOrganizationRequest();
        request.setStatus("PAUSED");

        assertThatThrownBy(() -> organizationsService.update(organization.getId(), request))
                .isInstanceOfSatisfying(FieldValidationException.class, exception ->
                        assertThat(exception.getFieldErrors()).containsOnly(entry("status", "INVALID_STATUS")));

        verify(organizationRepository, never()).save(any());
    }

    @Test
    void update_whenNameBelongsToAnotherOrganization_thenRejectTheNameField() {
        givenTheOrganizationExists();
        when(organizationRepository.existsByNameExcludingId("Liga Sur", organization.getId())).thenReturn(true);
        UpdateOrganizationRequest request = new UpdateOrganizationRequest();
        request.setName("Liga Sur");

        assertThatThrownBy(() -> organizationsService.update(organization.getId(), request))
                .isInstanceOfSatisfying(FieldValidationException.class, exception ->
                        assertThat(exception.getFieldErrors()).containsOnly(entry("name", "NAME_ALREADY_EXISTS")));
    }

    @Test
    void update_whenTheOrganizationDoesNotExist_thenNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(organizationRepository.findActiveById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationsService.update(unknownId, new UpdateOrganizationRequest()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ─── delete ─────────────────────────────────────────────────────────────────

    @Test
    void delete_whenNothingBelongsToIt_thenSoftDeleteAndFreeTheName() {
        givenTheOrganizationExists();

        organizationsService.delete(organization.getId());

        assertThat(organization.getDeletedAt()).isNotNull();
        assertThat(organization.getName()).contains("_DELETED_");
        verify(organizationRepository).delete(organization);
    }

    @Test
    void delete_whenItStillHasUsers_thenRefuseWithInUse() {
        givenTheOrganizationExists();
        when(userRepository.existsActiveByOrganizationId(organization.getId())).thenReturn(true);

        assertThatThrownBy(() -> organizationsService.delete(organization.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("IN_USE");

        verify(organizationRepository, never()).delete(any());
    }

    @Test
    void delete_whenItStillHasEvents_thenRefuseWithInUse() {
        givenTheOrganizationExists();
        when(eventRepository.existsActiveByOrganizationId(organization.getId())).thenReturn(true);

        assertThatThrownBy(() -> organizationsService.delete(organization.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("IN_USE");
    }

    @Test
    void delete_whenItStillHasTeams_thenRefuseWithInUse() {
        givenTheOrganizationExists();
        when(teamRepository.existsActiveByOrganizationId(organization.getId())).thenReturn(true);

        assertThatThrownBy(() -> organizationsService.delete(organization.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("IN_USE");
    }

    // ─── getAll ─────────────────────────────────────────────────────────────────

    @Test
    void getAll_whenCalled_thenReturnTheMappedList() {
        when(organizationRepository.findAllActive()).thenReturn(List.of(organization));

        List<OrganizationResponse> result = organizationsService.getAll();

        assertThat(result).extracting(OrganizationResponse::getName).containsExactly("Liga Norte");
    }
}
