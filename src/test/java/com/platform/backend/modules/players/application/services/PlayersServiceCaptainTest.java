package com.platform.backend.modules.players.application.services;

import com.platform.backend.modules.players.application.mappers.PlayerMapper;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.entities.TeamMembersEntity;
import com.platform.backend.modules.players.domain.exception.PlayerError;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.players.presentation.requests.UpdatePlayerCaptainRequest.UpdatePlayerCaptainRequest;
import com.platform.backend.modules.players.presentation.requests.UpdatePlayerRequest.UpdatePlayerRequest;
import com.platform.backend.modules.players.presentation.responses.PlayerResponse.PlayerResponse;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import com.platform.backend.modules.users.application.iservices.IPlayerAccountOrganizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InOrder;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The administrator designates the captain of a team: one per team, and only while the player belongs to it. A player can
 * play for several teams (one per organization) and is captain of each one separately.
 */
@ExtendWith(MockitoExtension.class)
class PlayersServiceCaptainTest {

    private static final UUID ORGANIZATION_ID = UUID.randomUUID();

    @Mock private IPlayerRepository playerRepository;
    @Mock private ITeamRepository teamRepository;
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
        memberships = spy(new InMemoryTeamMembers());
        PlayersServiceTestSupport.useInMemoryMemberships(playersService, playerMapper, memberships, playerAccountOrganizationService);

        tigres = team("Tigres", ORGANIZATION_ID);
        ana = player("ana@email.com");
        luis = player("luis@email.com");
        anaInTigres = memberships.put(ana, tigres);
        luisInTigres = memberships.put(luis, tigres);

        lenient().when(playerRepository.findActiveById(ana.getId())).thenReturn(Optional.of(ana));
        lenient().when(playerRepository.findActiveById(luis.getId())).thenReturn(Optional.of(luis));
        lenient().when(playerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static UpdatePlayerCaptainRequest captainRequest(boolean isCaptain) {
        UpdatePlayerCaptainRequest request = new UpdatePlayerCaptainRequest();
        request.setIsCaptain(isCaptain);
        return request;
    }

    @Test
    void updateCaptain_whenAPlayerOfATeamIsDesignated_thenTheyAreTheCaptain() {
        PlayerResponse response = playersService.updateCaptain(ana.getId(), captainRequest(true));

        assertThat(anaInTigres.getIsCaptain()).isTrue();
        assertThat(response.getIsCaptain()).isTrue();
    }

    @Test
    void updateCaptain_whenAnotherPlayerAlreadyIsTheCaptain_thenTheyStepDownFirst() {
        anaInTigres.setIsCaptain(true);

        playersService.updateCaptain(luis.getId(), captainRequest(true));

        assertThat(luisInTigres.getIsCaptain()).isTrue();
        assertThat(anaInTigres.getIsCaptain()).isFalse();
        // The previous captain is written before the new one: the unique index allows one captain per team.
        InOrder order = inOrder(memberships);
        order.verify(memberships).saveAndFlush(anaInTigres);
        order.verify(memberships).save(luisInTigres);
    }

    @Test
    void updateCaptain_whenThePlayerIsAlreadyTheCaptain_thenNothingElseChanges() {
        anaInTigres.setIsCaptain(true);

        playersService.updateCaptain(ana.getId(), captainRequest(true));

        assertThat(anaInTigres.getIsCaptain()).isTrue();
        verify(memberships, never()).saveAndFlush(any());
    }

    @Test
    void updateCaptain_whenTheCaptaincyIsRemoved_thenThePlayerIsNoLongerTheCaptain() {
        anaInTigres.setIsCaptain(true);

        playersService.updateCaptain(ana.getId(), captainRequest(false));

        assertThat(anaInTigres.getIsCaptain()).isFalse();
    }

    @Test
    void updateCaptain_whenThePlayerHasNoTeam_thenRefusedWithTheCode() {
        PlayersEntity free = player("libre@email.com");
        when(playerRepository.findActiveById(free.getId())).thenReturn(Optional.of(free));

        assertThatThrownBy(() -> playersService.updateCaptain(free.getId(), captainRequest(true)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(PlayerError.CAPTAIN_REQUIRES_TEAM.name());
    }

    // ─── a player of several teams ──────────────────────────────────────────────

    @Test
    void updateCaptain_whenThePlayerPlaysForTeamsOfTwoOrganizations_thenTheTeamHasToBeSaid() {
        memberships.put(ana, team("Pumas", UUID.randomUUID()));

        assertThatThrownBy(() -> playersService.updateCaptain(ana.getId(), captainRequest(true)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(PlayerError.TEAM_REQUIRED.name());
        assertThat(anaInTigres.getIsCaptain()).isFalse();
    }

    @Test
    void updateCaptain_whenTheTeamIsSaid_thenOnlyTheCaptaincyOfThatTeamChanges() {
        TeamsEntity pumas = team("Pumas", UUID.randomUUID());
        TeamMembersEntity anaInPumas = memberships.put(ana, pumas);
        anaInTigres.setIsCaptain(true);
        UpdatePlayerCaptainRequest request = captainRequest(true);
        request.setTeamId(pumas.getId());

        playersService.updateCaptain(ana.getId(), request);

        assertThat(anaInPumas.getIsCaptain()).isTrue();
        assertThat(anaInTigres.getIsCaptain()).isTrue();
    }

    @Test
    void updateCaptain_whenTheTeamSaidIsNotOneOfTheirs_thenRefusedAsHavingNoTeam() {
        UpdatePlayerCaptainRequest request = captainRequest(true);
        request.setTeamId(UUID.randomUUID());

        assertThatThrownBy(() -> playersService.updateCaptain(ana.getId(), request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(PlayerError.CAPTAIN_REQUIRES_TEAM.name());
    }

    // ─── leaving and changing team ──────────────────────────────────────────────

    @Test
    void removeFromTeam_whenTheCaptainLeaves_thenTheyAreNoLongerTheCaptain() {
        anaInTigres.setIsCaptain(true);

        playersService.removeFromTeam(ana.getId(), null);

        assertThat(memberships.activeTeamsOf(ana)).isEmpty();
        assertThat(anaInTigres.getIsCaptain()).isFalse();
        assertThat(anaInTigres.getDeletedAt()).isNotNull();
    }

    @Test
    void update_whenTheCaptainMovesToAnotherTeamOfTheSameOrganization_thenTheyAreNoLongerTheCaptain() {
        TeamsEntity leones = team("Leones", ORGANIZATION_ID);
        when(teamRepository.findActiveById(leones.getId())).thenReturn(Optional.of(leones));
        anaInTigres.setIsCaptain(true);
        UpdatePlayerRequest request = new UpdatePlayerRequest();
        request.setTeamId(leones.getId());

        playersService.update(ana.getId(), request);

        assertThat(memberships.activeTeamsOf(ana)).containsExactly(leones);
        assertThat(memberships.findActiveByPlayerIdAndTeamId(ana.getId(), leones.getId()).orElseThrow().getIsCaptain()).isFalse();
        assertThat(anaInTigres.getDeletedAt()).isNotNull();
    }

    @Test
    void update_whenTheCaptainIsPutInATeamOfAnotherOrganization_thenTheyKeepTheirOwnCaptaincy() {
        TeamsEntity pumas = team("Pumas", UUID.randomUUID());
        when(teamRepository.findActiveById(pumas.getId())).thenReturn(Optional.of(pumas));
        anaInTigres.setIsCaptain(true);
        UpdatePlayerRequest request = new UpdatePlayerRequest();
        request.setTeamId(pumas.getId());

        playersService.update(ana.getId(), request);

        assertThat(memberships.activeTeamsOf(ana)).containsExactlyInAnyOrder(tigres, pumas);
        assertThat(anaInTigres.getIsCaptain()).isTrue();
        assertThat(memberships.findActiveByPlayerIdAndTeamId(ana.getId(), pumas.getId()).orElseThrow().getIsCaptain()).isFalse();
    }

    @Test
    void update_whenTheCaptainIsPutInTheSameTeam_thenTheyKeepTheCaptaincy() {
        when(teamRepository.findActiveById(tigres.getId())).thenReturn(Optional.of(tigres));
        anaInTigres.setIsCaptain(true);
        UpdatePlayerRequest request = new UpdatePlayerRequest();
        request.setTeamId(tigres.getId());

        playersService.update(ana.getId(), request);

        assertThat(anaInTigres.getIsCaptain()).isTrue();
        assertThat(anaInTigres.getDeletedAt()).isNull();
    }
}
