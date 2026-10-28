package com.platform.backend.modules.players.application.services;

import com.platform.backend.modules.players.application.mappers.PlayerMapper;
import com.platform.backend.modules.players.application.mappers.TeamInvitationMapper;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.entities.TeamInvitationsEntity;
import com.platform.backend.modules.players.domain.enums.InvitationStatus;
import com.platform.backend.modules.players.domain.enums.InvitationType;
import com.platform.backend.modules.players.domain.exception.TeamInvitationError;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.players.domain.irepositories.ITeamInvitationRepository;
import com.platform.backend.modules.players.presentation.responses.AcceptedInvitationResponse.AcceptedInvitationResponse;
import com.platform.backend.modules.players.presentation.responses.PlayerResponse.PlayerResponse;
import com.platform.backend.modules.players.presentation.responses.TeamInvitationResponse.TeamInvitationResponse;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import com.platform.backend.modules.users.application.iservices.IPlayerAccountOrganizationService;
import com.platform.backend.modules.users.application.iservices.IUsersAuthService;
import com.platform.backend.modules.users.presentation.responses.AuthResponse.AuthResponse;
import com.platform.backend.shared.infraestructure.tenancy.TenantTestSupport;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamInvitationsServiceTest {

    private static final String MY_EMAIL = "juan@email.com";
    private static final UUID TEAM_ORGANIZATION_ID = UUID.randomUUID();
    private static final String FRESH_TOKEN = "token-with-the-new-organization";

    @Mock private ITeamInvitationRepository invitationRepository;
    @Mock private IPlayerRepository playerRepository;
    @Mock private ITeamRepository teamRepository;
    @Mock private IPlayerAccountOrganizationService playerAccountOrganizationService;
    @Mock private IUsersAuthService usersAuthService;
    @Spy private TeamInvitationMapper invitationMapper = Mappers.getMapper(TeamInvitationMapper.class);
    @Spy private PlayerMapper playerMapper = Mappers.getMapper(PlayerMapper.class);

    @InjectMocks
    private TeamInvitationsService teamInvitationsService;

    private TeamsEntity team;
    private PlayersEntity me;

    @BeforeEach
    void setUp() {
        TenantTestSupport.actAsMobileAccount(UUID.randomUUID(), MY_EMAIL);
        team = new TeamsEntity();
        team.setId(UUID.randomUUID());
        team.setName("Tigres");
        team.setOrganizationId(TEAM_ORGANIZATION_ID);
        me = new PlayersEntity();
        me.setId(UUID.randomUUID());
        me.setEmail(MY_EMAIL);

        lenient().when(teamRepository.findActiveById(team.getId())).thenReturn(Optional.of(team));
        lenient().when(usersAuthService.reissueSession(MY_EMAIL)).thenReturn(new AuthResponse(
                FRESH_TOKEN, false, false, TEAM_ORGANIZATION_ID, UUID.randomUUID(), MY_EMAIL, "Juan", null, "Perez", null));
        lenient().when(teamRepository.findAllActiveByIds(any())).thenReturn(List.of(team));
        lenient().when(playerRepository.findByEmail(MY_EMAIL)).thenReturn(Optional.of(me));
        lenient().when(playerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(invitationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void clearSecurityContext() {
        TenantTestSupport.actAsNobody();
    }

    private TeamInvitationsEntity invitation(InvitationType type, InvitationStatus status, LocalDateTime expiresAt) {
        TeamInvitationsEntity invitation = new TeamInvitationsEntity();
        invitation.setId(UUID.randomUUID());
        invitation.setEmail(MY_EMAIL);
        invitation.setTeamId(team.getId());
        invitation.setType(type.name());
        invitation.setStatus(status.name());
        invitation.setExpiresAt(expiresAt);
        return invitation;
    }

    private TeamInvitationsEntity pendingInvitation() {
        TeamInvitationsEntity invitation = invitation(InvitationType.EXISTING_USER, InvitationStatus.PENDING,
                LocalDateTime.now().plusDays(3));
        when(invitationRepository.findById(invitation.getId())).thenReturn(Optional.of(invitation));
        return invitation;
    }

    @Test
    void getMyPendingInvitations_whenThereAreSeveral_thenOnlyTheAnswerableOnesAreListedWithTheTeamName() {
        TeamInvitationsEntity answerable = invitation(InvitationType.EXISTING_USER, InvitationStatus.PENDING,
                LocalDateTime.now().plusDays(1));
        TeamInvitationsEntity expired = invitation(InvitationType.EXISTING_USER, InvitationStatus.PENDING,
                LocalDateTime.now().minusMinutes(1));
        TeamInvitationsEntity forRegistration = invitation(InvitationType.NEW_USER, InvitationStatus.PENDING,
                LocalDateTime.now().plusDays(1));
        when(invitationRepository.findAllActiveByEmailAndStatus(MY_EMAIL, "PENDING"))
                .thenReturn(List.of(answerable, expired, forRegistration));

        List<TeamInvitationResponse> invitations = teamInvitationsService.getMyPendingInvitations();

        assertThat(invitations).singleElement().satisfies(invitation -> {
            assertThat(invitation.getId()).isEqualTo(answerable.getId());
            assertThat(invitation.getTeamName()).isEqualTo("Tigres");
        });
    }

    @Test
    void accept_whenTheInvitationIsPending_thenThePlayerJoinsTheTeamAndItIsMarkedUsed() {
        TeamInvitationsEntity invitation = pendingInvitation();

        AcceptedInvitationResponse response = teamInvitationsService.accept(invitation.getId());

        assertThat(response.getPlayer().getTeamId()).isEqualTo(team.getId());
        assertThat(me.getTeam()).isSameAs(team);
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.USED.name());
    }

    @Test
    void accept_whenTheInvitationIsPending_thenTheAccountFollowsTheTeamAndTheAppGetsASessionForIt() {
        TeamInvitationsEntity invitation = pendingInvitation();

        AcceptedInvitationResponse response = teamInvitationsService.accept(invitation.getId());

        verify(playerAccountOrganizationService).followTeam(MY_EMAIL, TEAM_ORGANIZATION_ID);
        assertThat(response.getToken()).isEqualTo(FRESH_TOKEN);
    }

    @Test
    void accept_whenThePlayerWasInAnotherTeam_thenTheyMoveToTheNewOne() {
        TeamsEntity previous = new TeamsEntity();
        previous.setId(UUID.randomUUID());
        me.setTeam(previous);
        TeamInvitationsEntity invitation = pendingInvitation();

        teamInvitationsService.accept(invitation.getId());

        assertThat(me.getTeam()).isSameAs(team);
    }

    @Test
    void accept_whenTheInvitationWasSentToSomeoneElse_thenItDoesNotExistForTheCaller() {
        TeamInvitationsEntity invitation = pendingInvitation();
        invitation.setEmail("otra@email.com");

        assertThatThrownBy(() -> teamInvitationsService.accept(invitation.getId()))
                .isInstanceOf(EntityNotFoundException.class);
        verify(playerRepository, never()).save(any());
    }

    @Test
    void accept_whenTheEmailDiffersOnlyInCase_thenItIsTheCallersInvitation() {
        TeamInvitationsEntity invitation = pendingInvitation();
        invitation.setEmail("JUAN@Email.com");

        assertThat(teamInvitationsService.accept(invitation.getId()).getPlayer().getTeamId()).isEqualTo(team.getId());
    }

    @Test
    void accept_whenItWasAlreadyAnswered_thenRefusedWithTheCode() {
        TeamInvitationsEntity invitation = pendingInvitation();
        invitation.setStatus(InvitationStatus.DECLINED.name());

        assertThatThrownBy(() -> teamInvitationsService.accept(invitation.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(TeamInvitationError.NOT_PENDING.name());
    }

    @Test
    void accept_whenItExpired_thenRefusedWithTheCode() {
        TeamInvitationsEntity invitation = pendingInvitation();
        invitation.setExpiresAt(LocalDateTime.now().minusHours(1));

        assertThatThrownBy(() -> teamInvitationsService.accept(invitation.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(TeamInvitationError.EXPIRED.name());
    }

    @Test
    void accept_whenTheCallerHasNoPlayer_thenRefusedWithTheCodeAndTheInvitationStaysPending() {
        TeamInvitationsEntity invitation = pendingInvitation();
        when(playerRepository.findByEmail(MY_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamInvitationsService.accept(invitation.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(TeamInvitationError.PLAYER_NOT_FOUND.name());
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING.name());
    }

    @Test
    void accept_whenTheTeamNoLongerExists_thenRefusedWithTheCode() {
        TeamInvitationsEntity invitation = pendingInvitation();
        when(teamRepository.findActiveById(team.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamInvitationsService.accept(invitation.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(TeamInvitationError.TEAM_NOT_FOUND.name());
    }

    @Test
    void decline_whenTheInvitationIsPending_thenItIsMarkedDeclinedAndThePlayerKeepsTheirTeam() {
        TeamInvitationsEntity invitation = pendingInvitation();

        teamInvitationsService.decline(invitation.getId());

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.DECLINED.name());
        assertThat(me.getTeam()).isNull();
        verify(invitationRepository).save(invitation);
    }
}
