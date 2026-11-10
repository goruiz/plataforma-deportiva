package com.platform.backend.modules.players.application.services;

import com.platform.backend.modules.players.application.mappers.PlayerMapper;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.entities.TeamInvitationsEntity;
import com.platform.backend.modules.players.domain.enums.InvitationStatus;
import com.platform.backend.modules.players.domain.enums.InvitationType;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.players.domain.irepositories.ITeamInvitationRepository;
import com.platform.backend.modules.players.presentation.requests.InvitePlayerRequest.InvitePlayerRequest;
import com.platform.backend.modules.players.presentation.requests.InviteRegisterRequest.InviteRegisterRequest;
import com.platform.backend.modules.players.presentation.responses.PlayerResponse.PlayerResponse;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import com.platform.backend.modules.users.application.iservices.IPlayerAccountOrganizationService;
import com.platform.backend.shared.infraestructure.email.EmailService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * A person registered once is registered for the whole platform: an administrator of another organization who wants
 * them in a team finds them and sends them an invitation to accept, and never makes them register again.
 */
@ExtendWith(MockitoExtension.class)
class PlayersServiceInviteTest {

    private static final String EMAIL = "ana@email.com";

    @Mock private IPlayerRepository playerRepository;
    @Mock private ITeamRepository teamRepository;
    @Mock private ITeamInvitationRepository teamInvitationRepository;
    @Mock private EmailService emailService;
    @Mock private IPlayerAccountOrganizationService playerAccountOrganizationService;
    @Spy private PlayerMapper playerMapper = Mappers.getMapper(PlayerMapper.class);

    @InjectMocks
    private PlayersService playersService;

    private InMemoryTeamMembers memberships;
    private TeamsEntity invitingTeam;
    private PlayersEntity registeredPlayer;

    @BeforeEach
    void setUp() {
        memberships = new InMemoryTeamMembers();
        PlayersServiceTestSupport.useInMemoryMemberships(playersService, playerMapper, memberships, playerAccountOrganizationService);
        invitingTeam = new TeamsEntity();
        invitingTeam.setId(UUID.randomUUID());
        invitingTeam.setName("Los Tigres");

        // Ana already plays for a team of ANOTHER organization, with data that only that organization may see.
        TeamsEntity teamOfAnotherOrganization = new TeamsEntity();
        teamOfAnotherOrganization.setId(UUID.randomUUID());
        teamOfAnotherOrganization.setName("Los Leones");

        registeredPlayer = new PlayersEntity();
        registeredPlayer.setId(UUID.randomUUID());
        registeredPlayer.setEmail(EMAIL);
        registeredPlayer.setFirstName("Ana");
        registeredPlayer.setLastName("Gómez");
        registeredPlayer.setPhone("0991234567");
        registeredPlayer.setProfilePhotoUrl("players/photo");
        memberships.put(registeredPlayer, teamOfAnotherOrganization).setIsCaptain(true);
    }

    private InvitePlayerRequest invitePlayerRequest(String email) {
        InvitePlayerRequest request = new InvitePlayerRequest();
        request.setEmail(email);
        request.setTeamId(invitingTeam.getId());
        return request;
    }

    private InviteRegisterRequest inviteRegisterRequest(String email) {
        InviteRegisterRequest request = new InviteRegisterRequest();
        request.setEmail(email);
        request.setTeamId(invitingTeam.getId());
        return request;
    }

    // ─── searchByEmail ──────────────────────────────────────────────────────────

    @Test
    void searchByEmail_whenTheCallerSeesThePlayer_thenTheWholePlayerIsReturned() {
        when(playerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(registeredPlayer));

        PlayerResponse response = playersService.searchByEmail("Ana@Email.com");

        assertThat(response.getPhone()).isEqualTo("0991234567");
        assertThat(response.getTeamName()).isEqualTo("Los Leones");
        assertThat(response.getInTeamOfAnotherOrganization()).isNull();
        verify(playerRepository, never()).findAnyActiveByEmail(anyString());
    }

    @Test
    void searchByEmail_whenThePlayerPlaysForTheTeamOfAnotherOrganization_thenOnlyWhoTheyAreIsRevealed() {
        when(playerRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(playerRepository.findAnyActiveByEmail(EMAIL)).thenReturn(Optional.of(registeredPlayer));

        PlayerResponse response = playersService.searchByEmail("ana@email.com");

        assertThat(response.getFirstName()).isEqualTo("Ana");
        assertThat(response.getLastName()).isEqualTo("Gómez");
        assertThat(response.getEmail()).isEqualTo(EMAIL);
        assertThat(response.getProfilePhotoUrl()).isEqualTo("players/photo");
        assertThat(response.getInTeamOfAnotherOrganization()).isTrue();
        // What belongs to the organization of their team stays there.
        assertThat(response.getTeamId()).isNull();
        assertThat(response.getTeamName()).isNull();
        assertThat(response.getPhone()).isNull();
        assertThat(response.getIsCaptain()).isNull();
    }

    @Test
    void searchByEmail_whenNobodyIsRegisteredWithThatEmail_thenNotFound() {
        when(playerRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(playerRepository.findAnyActiveByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playersService.searchByEmail(EMAIL)).isInstanceOf(EntityNotFoundException.class);
    }

    // ─── invitePlayer ───────────────────────────────────────────────────────────

    @Test
    void invitePlayer_whenThePlayerPlaysForAnotherOrganization_thenTheyAreAskedToJoinAndNothingMovesThem() {
        when(playerRepository.findAnyActiveByEmail(EMAIL)).thenReturn(Optional.of(registeredPlayer));
        when(teamRepository.findActiveById(invitingTeam.getId())).thenReturn(Optional.of(invitingTeam));
        TeamsEntity currentTeam = memberships.activeTeamsOf(registeredPlayer).get(0);

        playersService.invitePlayer(invitePlayerRequest("Ana@EMAIL.com"));

        ArgumentCaptor<TeamInvitationsEntity> invitation = ArgumentCaptor.forClass(TeamInvitationsEntity.class);
        verify(teamInvitationRepository).save(invitation.capture());
        assertThat(invitation.getValue().getEmail()).isEqualTo(EMAIL);
        assertThat(invitation.getValue().getTeamId()).isEqualTo(invitingTeam.getId());
        assertThat(invitation.getValue().getType()).isEqualTo(InvitationType.EXISTING_USER.name());
        assertThat(invitation.getValue().getStatus()).isEqualTo(InvitationStatus.PENDING.name());
        verify(emailService).sendTeamInvitation(EMAIL, "Ana Gómez", "Los Tigres");
        // Only accepting the invitation moves the player.
        assertThat(memberships.activeTeamsOf(registeredPlayer)).containsExactly(currentTeam);
        verify(playerRepository, never()).save(any());
    }

    @Test
    void invitePlayer_whenNobodyIsRegisteredWithThatEmail_thenNotFoundWithoutInvitingNorEmailing() {
        when(playerRepository.findAnyActiveByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playersService.invitePlayer(invitePlayerRequest(EMAIL)))
                .isInstanceOf(EntityNotFoundException.class);

        verifyNoInteractions(teamInvitationRepository, emailService);
    }

    // ─── inviteRegister ─────────────────────────────────────────────────────────

    @Test
    void inviteRegister_whenThePersonIsAlreadyRegistered_thenTheyAreAskedToJoinInsteadOfToRegister() {
        when(teamRepository.findActiveById(invitingTeam.getId())).thenReturn(Optional.of(invitingTeam));
        when(playerRepository.findAnyActiveByEmail(EMAIL)).thenReturn(Optional.of(registeredPlayer));

        playersService.inviteRegister(inviteRegisterRequest("Ana@Email.com"));

        ArgumentCaptor<TeamInvitationsEntity> invitation = ArgumentCaptor.forClass(TeamInvitationsEntity.class);
        verify(teamInvitationRepository).save(invitation.capture());
        assertThat(invitation.getValue().getType()).isEqualTo(InvitationType.EXISTING_USER.name());
        assertThat(invitation.getValue().getToken()).isNull();
        verify(emailService).sendTeamInvitation(EMAIL, "Ana Gómez", "Los Tigres");
        verify(emailService, never()).sendRegistrationInvitation(anyString(), anyString(), anyString());
    }

    @Test
    void inviteRegister_whenNobodyIsRegisteredWithThatEmail_thenTheyGetTheLinkToRegister() {
        when(teamRepository.findActiveById(invitingTeam.getId())).thenReturn(Optional.of(invitingTeam));
        when(playerRepository.findAnyActiveByEmail(EMAIL)).thenReturn(Optional.empty());

        playersService.inviteRegister(inviteRegisterRequest("Ana@Email.com"));

        ArgumentCaptor<TeamInvitationsEntity> invitation = ArgumentCaptor.forClass(TeamInvitationsEntity.class);
        verify(teamInvitationRepository).save(invitation.capture());
        assertThat(invitation.getValue().getType()).isEqualTo(InvitationType.NEW_USER.name());
        assertThat(invitation.getValue().getToken()).isNotBlank();
        verify(emailService).sendRegistrationInvitation(EMAIL, "Los Tigres", invitation.getValue().getToken());
        verify(emailService, never()).sendTeamInvitation(anyString(), anyString(), anyString());
    }
}
