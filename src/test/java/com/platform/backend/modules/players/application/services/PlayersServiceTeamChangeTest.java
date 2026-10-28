package com.platform.backend.modules.players.application.services;

import com.platform.backend.modules.players.application.mappers.PlayerMapper;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.players.presentation.requests.UpdatePlayerRequest.UpdatePlayerRequest;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** When a player's team changes, their account follows it: what they see in the app is their team's organization. */
@ExtendWith(MockitoExtension.class)
class PlayersServiceTeamChangeTest {

    private static final String EMAIL = "ana@email.com";
    private static final UUID ORGANIZATION_ID = UUID.randomUUID();

    @Mock private IPlayerRepository playerRepository;
    @Mock private ITeamRepository teamRepository;
    @Mock private IPlayerAccountOrganizationService playerAccountOrganizationService;
    @Spy private PlayerMapper playerMapper = Mappers.getMapper(PlayerMapper.class);

    @InjectMocks
    private PlayersService playersService;

    private PlayersEntity player;
    private TeamsEntity team;

    @BeforeEach
    void setUp() {
        team = new TeamsEntity();
        team.setId(UUID.randomUUID());
        team.setOrganizationId(ORGANIZATION_ID);

        player = new PlayersEntity();
        player.setId(UUID.randomUUID());
        player.setEmail(EMAIL);

        when(playerRepository.findActiveById(player.getId())).thenReturn(Optional.of(player));
        when(playerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
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
    void removeFromTeam_whenThePlayerLeavesTheirTeam_thenTheirAccountLosesTheOrganization() {
        player.setTeam(team);

        playersService.removeFromTeam(player.getId());

        verify(playerAccountOrganizationService).followTeam(EMAIL, null);
    }
}
