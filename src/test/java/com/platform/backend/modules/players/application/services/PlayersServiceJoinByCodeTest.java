package com.platform.backend.modules.players.application.services;

import com.platform.backend.modules.players.application.mappers.PlayerMapper;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.exception.PlayerError;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.players.domain.irepositories.ITeamInvitationRepository;
import com.platform.backend.modules.players.presentation.requests.JoinByCodeRequest.JoinByCodeRequest;
import com.platform.backend.modules.players.presentation.responses.AcceptedInvitationResponse.AcceptedInvitationResponse;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.enums.TeamStatus;
import com.platform.backend.modules.teams.domain.exception.TeamError;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import com.platform.backend.modules.users.application.iservices.IEmailVerificationService;
import com.platform.backend.modules.users.application.iservices.IPlayerAccountOrganizationService;
import com.platform.backend.modules.users.application.iservices.IUsersAuthService;
import com.platform.backend.modules.users.presentation.responses.AuthResponse.AuthResponse;
import com.platform.backend.shared.domain.exception.AccessError;
import com.platform.backend.shared.infraestructure.email.EmailService;
import com.platform.backend.shared.infraestructure.tenancy.TenantTestSupport;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mapstruct.factory.Mappers;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static com.platform.backend.modules.players.application.services.PlayersServiceTestSupport.team;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * A player joins the team whose code they type, like accepting an invitation. A player has one team per organization: a
 * code is not a way to change teams inside an organization, but a team of another organization is simply added.
 */
@ExtendWith(MockitoExtension.class)
class PlayersServiceJoinByCodeTest {

    private static final String EMAIL = "ana@email.com";
    private static final String FRESH_TOKEN = "token-with-the-new-organization";
    private static final UUID TEAM_ORGANIZATION_ID = UUID.randomUUID();

    @Mock private IPlayerRepository playerRepository;
    @Mock private ITeamRepository teamRepository;
    @Mock private ITeamInvitationRepository teamInvitationRepository;
    @Mock private IUsersAuthService usersAuthService;
    @Mock private IEmailVerificationService emailVerificationService;
    @Mock private IPlayerAccountOrganizationService playerAccountOrganizationService;
    @Mock private EmailService emailService;
    @Spy private PlayerMapper playerMapper = Mappers.getMapper(PlayerMapper.class);

    @InjectMocks
    private PlayersService playersService;

    private InMemoryTeamMembers memberships;
    private TeamsEntity team;
    private PlayersEntity player;

    @BeforeEach
    void setUp() {
        TenantTestSupport.actAsMobileAccount(UUID.randomUUID(), EMAIL);
        memberships = new InMemoryTeamMembers();
        PlayersServiceTestSupport.useInMemoryMemberships(playersService, playerMapper, memberships, playerAccountOrganizationService);

        team = team("Tigres", TEAM_ORGANIZATION_ID);
        team.setJoinCode("AB3D9FK2");

        player = new PlayersEntity();
        player.setId(UUID.randomUUID());
        player.setEmail(EMAIL);
        player.setFirstName("Ana");
        player.setLastName("Gómez");

        lenient().when(playerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(player));
        lenient().when(teamRepository.findActiveByJoinCode("AB3D9FK2")).thenReturn(Optional.of(team));
        lenient().when(usersAuthService.reissueSession(EMAIL)).thenReturn(new AuthResponse(
                FRESH_TOKEN, false, false, TEAM_ORGANIZATION_ID, UUID.randomUUID(), EMAIL, "Ana", null, "Gómez", null, true));
    }

    @AfterEach
    void clearSecurityContext() {
        TenantTestSupport.actAsNobody();
    }

    private static JoinByCodeRequest requestWith(String code) {
        JoinByCodeRequest request = new JoinByCodeRequest();
        request.setCode(code);
        return request;
    }

    @Test
    void joinByCode_whenTheCodeIsTypedWithHyphenAndLowerCase_thenThePlayerJoinsTheTeamAndGetsANewToken() {
        AcceptedInvitationResponse response = playersService.joinByCode(requestWith(" ab3d-9fk2 "));

        assertThat(memberships.activeTeamsOf(player)).containsExactly(team);
        assertThat(response.getPlayer().getTeamId()).isEqualTo(team.getId());
        assertThat(response.getPlayer().getTeams()).extracting("teamId").containsExactly(team.getId());
        assertThat(response.getToken()).isEqualTo(FRESH_TOKEN);
        // What the player sees follows their teams: the account moves to the team's organization.
        verify(playerAccountOrganizationService).followTeam(EMAIL, TEAM_ORGANIZATION_ID);
    }

    @Test
    void joinByCode_whenTheEmailIsNotConfirmed_thenItIsRefusedBeforeLookingAnythingUp() {
        doThrow(new AccessDeniedException(AccessError.EMAIL_NOT_VERIFIED.name()))
                .when(emailVerificationService).requireVerified(EMAIL);

        assertThatThrownBy(() -> playersService.joinByCode(requestWith("AB3D9FK2")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage(AccessError.EMAIL_NOT_VERIFIED.name());

        verifyNoInteractions(teamRepository);
        assertThat(memberships.activeTeamsOf(player)).isEmpty();
    }

    @Test
    void joinByCode_whenThePlayerAlreadyHasATeamInThatOrganization_thenItIsRefusedAndTheirTeamIsKept() {
        TeamsEntity currentTeam = team("Leones", TEAM_ORGANIZATION_ID);
        memberships.put(player, currentTeam);

        assertThatThrownBy(() -> playersService.joinByCode(requestWith("AB3D9FK2")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(PlayerError.ALREADY_IN_TEAM.name());

        assertThat(memberships.activeTeamsOf(player)).containsExactly(currentTeam);
    }

    @Test
    void joinByCode_whenThePlayerAlreadyPlaysForThatSameTeam_thenItIsRefusedToo() {
        memberships.put(player, team);

        assertThatThrownBy(() -> playersService.joinByCode(requestWith("AB3D9FK2")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(PlayerError.ALREADY_IN_TEAM.name());
    }

    @Test
    void joinByCode_whenThePlayerHasATeamInAnotherOrganization_thenTheNewTeamIsAddedAndTheOldOneStays() {
        TeamsEntity teamElsewhere = team("Pumas", UUID.randomUUID());
        memberships.put(player, teamElsewhere);

        AcceptedInvitationResponse response = playersService.joinByCode(requestWith("AB3D9FK2"));

        assertThat(memberships.activeTeamsOf(player)).containsExactlyInAnyOrder(teamElsewhere, team);
        // Whoever reads a single team sees the one joined last; the list has both.
        assertThat(response.getPlayer().getTeamId()).isEqualTo(team.getId());
        assertThat(response.getPlayer().getTeams()).hasSize(2);
    }

    @Test
    void joinByCode_whenNoTeamHasThatCode_thenItIsNotFoundWithTheCode() {
        assertThatThrownBy(() -> playersService.joinByCode(requestWith("ZZZZZZZZ")))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage(TeamError.JOIN_CODE_NOT_FOUND.name());

        assertThat(memberships.activeTeamsOf(player)).isEmpty();
        verifyNoInteractions(playerAccountOrganizationService);
    }

    @Test
    void joinByCode_whenTheTeamIsNoLongerActive_thenItAnswersAsIfTheCodeDidNotExist() {
        team.setStatus(TeamStatus.INACTIVE.name());

        assertThatThrownBy(() -> playersService.joinByCode(requestWith("AB3D9FK2")))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage(TeamError.JOIN_CODE_NOT_FOUND.name());

        assertThat(memberships.activeTeamsOf(player)).isEmpty();
    }

    @Test
    void joinByCode_whenTheAccountHasNoPlayer_thenItIsNotFound() {
        when(playerRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playersService.joinByCode(requestWith("AB3D9FK2")))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
