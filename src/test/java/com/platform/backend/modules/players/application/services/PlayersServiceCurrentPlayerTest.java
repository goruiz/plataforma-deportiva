package com.platform.backend.modules.players.application.services;

import com.platform.backend.modules.players.application.mappers.PlayerMapper;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.players.presentation.responses.PlayerResponse.PlayerResponse;
import com.platform.backend.modules.users.application.iservices.IPlayerAccountOrganizationService;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** {@code GET /players/me}: the player that goes with the account that is calling. */
@ExtendWith(MockitoExtension.class)
class PlayersServiceCurrentPlayerTest {

    private static final String MY_EMAIL = "juan@email.com";

    @Mock private IPlayerRepository playerRepository;
    @Mock private IPlayerAccountOrganizationService playerAccountOrganizationService;
    @Spy private PlayerMapper playerMapper = Mappers.getMapper(PlayerMapper.class);

    @InjectMocks
    private PlayersService playersService;

    private final InMemoryTeamMembers memberships = new InMemoryTeamMembers();

    @BeforeEach
    void actAsTheAccount() {
        PlayersServiceTestSupport.useInMemoryMemberships(playersService, playerMapper, memberships, playerAccountOrganizationService);
        TenantTestSupport.actAsMobileAccount(UUID.randomUUID(), MY_EMAIL);
    }

    @AfterEach
    void clearSecurityContext() {
        TenantTestSupport.actAsNobody();
    }

    private static PlayersEntity playerWithEmail(String email) {
        PlayersEntity player = new PlayersEntity();
        player.setId(UUID.randomUUID());
        player.setEmail(email);
        return player;
    }

    @Test
    void getCurrent_whenThereIsAPlayerWithTheAccountsEmail_thenItIsReturned() {
        PlayersEntity me = playerWithEmail(MY_EMAIL);
        when(playerRepository.findByEmail(MY_EMAIL)).thenReturn(Optional.of(me));

        PlayerResponse response = playersService.getCurrent();

        assertThat(response.getId()).isEqualTo(me.getId());
        assertThat(response.getEmail()).isEqualTo(MY_EMAIL);
    }

    @Test
    void getCurrent_whenThePlayerPlaysForTeamsOfSeveralOrganizations_thenEveryTeamIsListedTheLastJoinedFirst() {
        PlayersEntity me = playerWithEmail(MY_EMAIL);
        com.platform.backend.modules.teams.domain.entities.TeamsEntity first =
                PlayersServiceTestSupport.team("Leones", UUID.randomUUID());
        com.platform.backend.modules.teams.domain.entities.TeamsEntity second =
                PlayersServiceTestSupport.team("Tigres", UUID.randomUUID());
        memberships.put(me, first);
        memberships.put(me, second).setIsCaptain(true);
        when(playerRepository.findByEmail(MY_EMAIL)).thenReturn(Optional.of(me));

        PlayerResponse response = playersService.getCurrent();

        assertThat(response.getTeams()).extracting("teamName").containsExactly("Tigres", "Leones");
        // A client that knows a single team reads the one joined last, with its captaincy.
        assertThat(response.getTeamId()).isEqualTo(second.getId());
        assertThat(response.getIsCaptain()).isTrue();
    }

    @Test
    void getCurrent_whenThereIsNoPlayerForTheAccount_thenNotFound() {
        when(playerRepository.findByEmail(MY_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playersService.getCurrent()).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getCurrent_whenThePlayerWasDeleted_thenNotFound() {
        PlayersEntity deleted = playerWithEmail(MY_EMAIL);
        deleted.setDeletedAt(LocalDateTime.now());
        when(playerRepository.findByEmail(MY_EMAIL)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> playersService.getCurrent()).isInstanceOf(EntityNotFoundException.class);
    }
}
