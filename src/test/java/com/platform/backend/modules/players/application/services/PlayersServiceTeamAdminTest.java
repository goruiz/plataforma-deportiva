package com.platform.backend.modules.players.application.services;

import com.platform.backend.modules.players.application.mappers.PlayerMapper;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.entities.TeamMembersEntity;
import com.platform.backend.modules.players.domain.exception.PlayerError;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.players.presentation.requests.UpdatePlayerTeamAdminRequest.UpdatePlayerTeamAdminRequest;
import com.platform.backend.modules.players.presentation.responses.PlayerResponse.PlayerResponse;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.users.application.iservices.IPlayerAccountOrganizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.platform.backend.modules.players.application.services.PlayersServiceTestSupport.player;
import static com.platform.backend.modules.players.application.services.PlayersServiceTestSupport.team;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * The administrator of the platform names the administrators of a team: any number per team, only while the player
 * belongs to it, and the role ends with the membership.
 */
@ExtendWith(MockitoExtension.class)
class PlayersServiceTeamAdminTest {

    private static final UUID ORGANIZATION_ID = UUID.randomUUID();

    @Mock private IPlayerRepository playerRepository;
    @Mock private IPlayerAccountOrganizationService playerAccountOrganizationService;
    @Spy private PlayerMapper playerMapper = Mappers.getMapper(PlayerMapper.class);

    @InjectMocks
    private PlayersService playersService;

    private InMemoryTeamMembers memberships;
    private TeamsEntity tigres;
    private PlayersEntity ana;
    private PlayersEntity luis;
    private TeamMembersEntity anaInTigres;
    private TeamMembersEntity luisInTigres;

    @BeforeEach
    void setUp() {
        memberships = new InMemoryTeamMembers();
        PlayersServiceTestSupport.useInMemoryMemberships(playersService, playerMapper, memberships, playerAccountOrganizationService);

        tigres = team("Tigres", ORGANIZATION_ID);
        ana = player("ana@email.com");
        luis = player("luis@email.com");
        anaInTigres = memberships.put(ana, tigres);
        luisInTigres = memberships.put(luis, tigres);

        lenient().when(playerRepository.findActiveById(ana.getId())).thenReturn(Optional.of(ana));
        lenient().when(playerRepository.findActiveById(luis.getId())).thenReturn(Optional.of(luis));
    }

    private static UpdatePlayerTeamAdminRequest request(boolean isTeamAdmin) {
        UpdatePlayerTeamAdminRequest request = new UpdatePlayerTeamAdminRequest();
        request.setIsTeamAdmin(isTeamAdmin);
        return request;
    }

    @Test
    void updateTeamAdmin_whenAPlayerOfATeamIsNamed_thenTheyAdministerTheTeam() {
        PlayerResponse response = playersService.updateTeamAdmin(ana.getId(), request(true));

        assertThat(anaInTigres.isAdministratorOfTheTeam()).isTrue();
        assertThat(response.getIsTeamAdmin()).isTrue();
        assertThat(response.getTeams()).singleElement().satisfies(team -> assertThat(team.getIsTeamAdmin()).isTrue());
    }

    @Test
    void updateTeamAdmin_whenAnotherPlayerAlreadyAdministersTheTeam_thenBothDoAndNobodyStepsDown() {
        playersService.updateTeamAdmin(ana.getId(), request(true));

        playersService.updateTeamAdmin(luis.getId(), request(true));

        assertThat(anaInTigres.isAdministratorOfTheTeam()).isTrue();
        assertThat(luisInTigres.isAdministratorOfTheTeam()).isTrue();
    }

    @Test
    void updateTeamAdmin_whenTheRoleIsRemoved_thenThePlayerIsAnOrdinaryMemberAgain() {
        anaInTigres.setIsTeamAdmin(true);

        playersService.updateTeamAdmin(ana.getId(), request(false));

        assertThat(anaInTigres.isAdministratorOfTheTeam()).isFalse();
    }

    @Test
    void updateTeamAdmin_whenTheCaptainIsNamedAdministrator_thenTheCaptaincyIsUntouched() {
        anaInTigres.setIsCaptain(true);

        playersService.updateTeamAdmin(ana.getId(), request(true));

        assertThat(anaInTigres.isCaptainOfTheTeam()).isTrue();
        assertThat(anaInTigres.isAdministratorOfTheTeam()).isTrue();
    }

    @Test
    void updateTeamAdmin_whenThePlayerHasNoTeam_thenRefusedWithTheCode() {
        PlayersEntity free = player("libre@email.com");
        when(playerRepository.findActiveById(free.getId())).thenReturn(Optional.of(free));

        assertThatThrownBy(() -> playersService.updateTeamAdmin(free.getId(), request(true)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(PlayerError.TEAM_ADMIN_REQUIRES_TEAM.name());
    }

    @Test
    void updateTeamAdmin_whenThePlayerPlaysForTeamsOfTwoOrganizations_thenTheTeamHasToBeSaid() {
        memberships.put(ana, team("Pumas", UUID.randomUUID()));

        assertThatThrownBy(() -> playersService.updateTeamAdmin(ana.getId(), request(true)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(PlayerError.TEAM_REQUIRED.name());
        assertThat(anaInTigres.isAdministratorOfTheTeam()).isFalse();
    }

    @Test
    void updateTeamAdmin_whenTheTeamIsSaid_thenOnlyTheRoleInThatTeamChanges() {
        TeamsEntity pumas = team("Pumas", UUID.randomUUID());
        TeamMembersEntity anaInPumas = memberships.put(ana, pumas);
        UpdatePlayerTeamAdminRequest request = request(true);
        request.setTeamId(pumas.getId());

        playersService.updateTeamAdmin(ana.getId(), request);

        assertThat(anaInPumas.isAdministratorOfTheTeam()).isTrue();
        assertThat(anaInTigres.isAdministratorOfTheTeam()).isFalse();
    }

    @Test
    void removeFromTeam_whenAnAdministratorOfTheTeamLeaves_thenTheyStopBeingOne() {
        anaInTigres.setIsTeamAdmin(true);

        playersService.removeFromTeam(ana.getId(), null);

        assertThat(anaInTigres.isAdministratorOfTheTeam()).isFalse();
        assertThat(anaInTigres.getDeletedAt()).isNotNull();
        assertThat(memberships.existsActiveTeamAdministrator(tigres.getId(), "ana@email.com")).isFalse();
    }

    @Test
    void anAdministratorOfOneTeamIsNotOneOfAnother() {
        anaInTigres.setIsTeamAdmin(true);

        assertThat(memberships.existsActiveTeamAdministrator(tigres.getId(), "ana@email.com")).isTrue();
        assertThat(memberships.existsActiveTeamAdministrator(UUID.randomUUID(), "ana@email.com")).isFalse();
    }
}
