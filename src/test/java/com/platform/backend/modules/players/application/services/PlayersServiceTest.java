package com.platform.backend.modules.players.application.services;

import com.platform.backend.modules.players.application.mappers.PlayerMapper;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.players.domain.irepositories.ITeamInvitationRepository;
import com.platform.backend.modules.players.presentation.responses.PlayerResponse.PlayerResponse;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import com.platform.backend.shared.infraestructure.email.EmailService;
import com.platform.backend.shared.infraestructure.tenancy.TenantTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayersServiceTest {

    @Mock private IPlayerRepository playerRepository;
    @Mock private ITeamRepository teamRepository;
    @Mock private ITeamInvitationRepository teamInvitationRepository;
    @Mock private PlayerMapper playerMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    @InjectMocks
    private PlayersService playersService;

    private PlayersEntity playerOfATeam;
    private PlayersEntity playerWithoutTeam;

    @BeforeEach
    void setUp() {
        playerOfATeam = player("con-equipo@email.com");
        playerOfATeam.setTeam(new TeamsEntity());
        playerWithoutTeam = player("sin-equipo@email.com");

        when(playerRepository.findAllActive()).thenReturn(List.of(playerOfATeam, playerWithoutTeam));
        lenient().when(playerMapper.toResponse(any(PlayersEntity.class))).thenAnswer(invocation -> {
            PlayerResponse response = new PlayerResponse();
            response.setEmail(((PlayersEntity) invocation.getArgument(0)).getEmail());
            return response;
        });
    }

    @AfterEach
    void clearSecurityContext() {
        TenantTestSupport.actAsNobody();
    }

    private static PlayersEntity player(String email) {
        PlayersEntity player = new PlayersEntity();
        player.setId(UUID.randomUUID());
        player.setEmail(email);
        return player;
    }

    @Test
    void getAll_whenAnOrganizationAdministratorAsks_thenOnlyPlayersOfTeamsAreListed() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());

        assertThat(playersService.getAll()).extracting(PlayerResponse::getEmail).containsExactly("con-equipo@email.com");
    }

    @Test
    void getAll_whenThePlatformOwnerAsks_thenEveryPlayerIsListed() {
        TenantTestSupport.actAsPlatformOwner();

        assertThat(playersService.getAll()).extracting(PlayerResponse::getEmail)
                .containsExactly("con-equipo@email.com", "sin-equipo@email.com");
    }

    @Test
    void getAll_whenAMobileAccountAsks_thenNothingChangesForThem() {
        TenantTestSupport.actAsMobileAccountWithoutOrganization();

        assertThat(playersService.getAll()).hasSize(2);
    }
}
