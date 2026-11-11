package com.platform.backend.modules.players.application.services;

import com.platform.backend.modules.players.application.mappers.PlayerMapper;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.players.presentation.requests.UpdatePlayerRequest.UpdatePlayerRequest;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import com.platform.backend.modules.users.application.iservices.IPlayerAccountOrganizationService;
import com.platform.backend.shared.domain.exception.AccessError;
import com.platform.backend.shared.infraestructure.tenancy.TenantTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static com.platform.backend.modules.players.application.services.PlayersServiceTestSupport.player;
import static com.platform.backend.modules.players.application.services.PlayersServiceTestSupport.team;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * When a player's teams change, their account follows the one they joined last: what they see in the app is read from all
 * their teams, but the organization of the account is a single one.
 */
@ExtendWith(MockitoExtension.class)
class PlayersServiceTeamChangeTest {

    private static final String EMAIL = "ana@email.com";
    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final UUID OTHER_ORGANIZATION_ID = UUID.randomUUID();

    @Mock private IPlayerRepository playerRepository;
    @Mock private ITeamRepository teamRepository;
    @Mock private IPlayerAccountOrganizationService playerAccountOrganizationService;
    @Spy private PlayerMapper playerMapper = Mappers.getMapper(PlayerMapper.class);

    @InjectMocks
    private PlayersService playersService;

    private InMemoryTeamMembers memberships;
    private PlayersEntity player;
    private TeamsEntity team;

    @BeforeEach
    void setUp() {
        memberships = new InMemoryTeamMembers();
        PlayersServiceTestSupport.useInMemoryMemberships(playersService, playerMapper, memberships, playerAccountOrganizationService);

        team = team("Tigres", ORGANIZATION_ID);
        player = player(EMAIL);

        when(playerRepository.findActiveById(player.getId())).thenReturn(Optional.of(player));
        lenient().when(playerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void update_whenThePlayerIsGivenATeam_thenTheirAccountTakesTheOrganizationOfThatTeam() {
        when(teamRepository.findActiveById(team.getId())).thenReturn(Optional.of(team));
        UpdatePlayerRequest request = new UpdatePlayerRequest();
        request.setTeamId(team.getId());

        playersService.update(player.getId(), request);

        verify(playerAccountOrganizationService).followTeam(EMAIL, ORGANIZATION_ID);
    }

    @Test
    void update_whenTheTeamIsNotTouched_thenTheAccountIsLeftAlone() {
        playersService.update(player.getId(), new UpdatePlayerRequest());

        verify(playerAccountOrganizationService, never()).followTeam(any(), any());
    }

    @Test
    void removeFromTeam_whenThePlayerLeavesTheirOnlyTeam_thenTheirAccountLosesTheOrganization() {
        memberships.put(player, team);

        playersService.removeFromTeam(player.getId(), null);

        verify(playerAccountOrganizationService).followTeam(EMAIL, null);
    }

    @Test
    void removeFromTeam_whenThePlayerStillPlaysForATeamOfAnotherOrganization_thenTheirAccountFollowsThatOne() {
        memberships.put(player, team("Pumas", OTHER_ORGANIZATION_ID));
        memberships.put(player, team);

        playersService.removeFromTeam(player.getId(), team.getId());

        verify(playerAccountOrganizationService).followTeam(EMAIL, OTHER_ORGANIZATION_ID);
    }

    // ─── Who may take a player out of a team ────────────────────────────────────

    @Test
    void removeFromTeam_whenThePlayerLeavesByThemselves_thenTheyAreOut() {
        TenantTestSupport.actAsMobileAccount(UUID.randomUUID(), EMAIL);
        memberships.put(player, team);

        playersService.removeFromTeam(player.getId(), team.getId());

        verify(playerAccountOrganizationService).followTeam(EMAIL, null);
    }

    @Test
    void removeFromTeam_whenAnAdministratorTakesThePlayerOut_thenTheyAreOut() {
        TenantTestSupport.actAsOrganizationAdmin(ORGANIZATION_ID);
        memberships.put(player, team);

        playersService.removeFromTeam(player.getId(), team.getId());

        verify(playerAccountOrganizationService).followTeam(EMAIL, null);
    }

    @Test
    void removeFromTeam_whenAnotherPlayerTriesToTakeThemOut_thenItIsRefusedAsNotYourself() {
        TenantTestSupport.actAsMobileAccount(UUID.randomUUID(), "rival@email.com");
        memberships.put(player, team);

        assertThatThrownBy(() -> playersService.removeFromTeam(player.getId(), team.getId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage(AccessError.NOT_YOURSELF.name());
        verify(playerAccountOrganizationService, never()).followTeam(any(), any());
    }

    @AfterEach
    void clearSecurityContext() {
        TenantTestSupport.actAsNobody();
    }
}
