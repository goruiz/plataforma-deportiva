package com.platform.backend.modules.players.application.services;

import com.platform.backend.modules.players.application.mappers.PlayerMapper;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The administrator designates the captain of a team: one per team, and only while the player belongs to it. */
@ExtendWith(MockitoExtension.class)
class PlayersServiceCaptainTest {

    @Mock private IPlayerRepository playerRepository;
    @Mock private ITeamRepository teamRepository;
    @Mock private IPlayerAccountOrganizationService playerAccountOrganizationService;
    @Spy private PlayerMapper playerMapper = Mappers.getMapper(PlayerMapper.class);

    @InjectMocks
    private PlayersService playersService;

    private TeamsEntity tigres;
    private PlayersEntity ana;
    private PlayersEntity luis;

    @BeforeEach
    void setUp() {
        tigres = team();
        ana = playerOf(tigres, "ana@email.com");
        luis = playerOf(tigres, "luis@email.com");

        lenient().when(playerRepository.findActiveById(ana.getId())).thenReturn(Optional.of(ana));
        lenient().when(playerRepository.findActiveById(luis.getId())).thenReturn(Optional.of(luis));
        lenient().when(playerRepository.findAllActiveByTeamId(tigres.getId())).thenReturn(List.of(ana, luis));
        lenient().when(playerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static TeamsEntity team() {
        TeamsEntity team = new TeamsEntity();
        team.setId(UUID.randomUUID());
        return team;
    }

    private static PlayersEntity playerOf(TeamsEntity team, String email) {
        PlayersEntity player = new PlayersEntity();
        player.setId(UUID.randomUUID());
        player.setEmail(email);
        player.setTeam(team);
        return player;
    }

    private static UpdatePlayerCaptainRequest captainRequest(boolean isCaptain) {
        UpdatePlayerCaptainRequest request = new UpdatePlayerCaptainRequest();
        request.setIsCaptain(isCaptain);
        return request;
    }

    @Test
    void updateCaptain_whenAPlayerOfATeamIsDesignated_thenTheyAreTheCaptain() {
        PlayerResponse response = playersService.updateCaptain(ana.getId(), captainRequest(true));

        assertThat(ana.getIsCaptain()).isTrue();
        assertThat(response.getIsCaptain()).isTrue();
    }

    @Test
    void updateCaptain_whenAnotherPlayerAlreadyIsTheCaptain_thenTheyStepDownFirst() {
        ana.setIsCaptain(true);

        playersService.updateCaptain(luis.getId(), captainRequest(true));

        assertThat(luis.getIsCaptain()).isTrue();
        assertThat(ana.getIsCaptain()).isFalse();
        // The previous captain is written before the new one: the unique index allows one captain per team.
        InOrder order = inOrder(playerRepository);
        order.verify(playerRepository).saveAndFlush(ana);
        order.verify(playerRepository).save(luis);
    }

    @Test
    void updateCaptain_whenThePlayerIsAlreadyTheCaptain_thenNothingElseChanges() {
        ana.setIsCaptain(true);

        playersService.updateCaptain(ana.getId(), captainRequest(true));

        assertThat(ana.getIsCaptain()).isTrue();
        verify(playerRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateCaptain_whenTheCaptaincyIsRemoved_thenThePlayerIsNoLongerTheCaptain() {
        ana.setIsCaptain(true);

        playersService.updateCaptain(ana.getId(), captainRequest(false));

        assertThat(ana.getIsCaptain()).isFalse();
    }

    @Test
    void updateCaptain_whenThePlayerHasNoTeam_thenRefusedWithTheCode() {
        PlayersEntity free = playerOf(null, "libre@email.com");
        when(playerRepository.findActiveById(free.getId())).thenReturn(Optional.of(free));

        assertThatThrownBy(() -> playersService.updateCaptain(free.getId(), captainRequest(true)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(PlayerError.CAPTAIN_REQUIRES_TEAM.name());
        assertThat(free.getIsCaptain()).isFalse();
    }

    @Test
    void removeFromTeam_whenTheCaptainLeaves_thenTheyAreNoLongerTheCaptain() {
        ana.setIsCaptain(true);

        playersService.removeFromTeam(ana.getId());

        assertThat(ana.getTeam()).isNull();
        assertThat(ana.getIsCaptain()).isFalse();
    }

    @Test
    void update_whenTheCaptainMovesToAnotherTeam_thenTheyAreNoLongerTheCaptain() {
        TeamsEntity leones = team();
        when(teamRepository.findActiveById(leones.getId())).thenReturn(Optional.of(leones));
        ana.setIsCaptain(true);
        UpdatePlayerRequest request = new UpdatePlayerRequest();
        request.setTeamId(leones.getId());

        playersService.update(ana.getId(), request);

        assertThat(ana.getTeam()).isSameAs(leones);
        assertThat(ana.getIsCaptain()).isFalse();
    }

    @Test
    void update_whenTheCaptainIsPutInTheSameTeam_thenTheyKeepTheCaptaincy() {
        when(teamRepository.findActiveById(tigres.getId())).thenReturn(Optional.of(tigres));
        ana.setIsCaptain(true);
        UpdatePlayerRequest request = new UpdatePlayerRequest();
        request.setTeamId(tigres.getId());

        playersService.update(ana.getId(), request);

        assertThat(ana.getIsCaptain()).isTrue();
    }
}
