package com.platform.backend.modules.players.application.services;

import com.platform.backend.modules.players.application.mappers.PlayerMapper;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.entities.TeamInvitationsEntity;
import com.platform.backend.modules.players.domain.enums.InvitationStatus;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.players.domain.irepositories.ITeamInvitationRepository;
import com.platform.backend.modules.players.presentation.requests.CreatePlayerRequest.CreatePlayerRequest;
import com.platform.backend.modules.players.presentation.requests.InvitePlayerRequest.InvitePlayerRequest;
import com.platform.backend.modules.players.presentation.requests.PlayerRegisterRequest.PlayerRegisterRequest;
import com.platform.backend.modules.players.presentation.responses.PlayerResponse.PlayerResponse;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import com.platform.backend.modules.users.application.iservices.IUsersAuthService;
import com.platform.backend.modules.users.presentation.requests.RegisterRequest.RegisterRequest;
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
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Whoever plays in the app is an account and a player with the same email: registering a player (with or without an
 * invitation) creates the account through the same registration the app uses, so they can sign in afterwards.
 */
@ExtendWith(MockitoExtension.class)
class PlayersServiceRegistrationTest {

    private static final String TOKEN = "invitation-token";
    private static final String EMAIL = "ana@email.com";

    @Mock private IPlayerRepository playerRepository;
    @Mock private ITeamRepository teamRepository;
    @Mock private ITeamInvitationRepository teamInvitationRepository;
    @Mock private IUsersAuthService usersAuthService;
    @Mock private EmailService emailService;
    @Spy private PlayerMapper playerMapper = Mappers.getMapper(PlayerMapper.class);

    @InjectMocks
    private PlayersService playersService;

    private TeamsEntity team;
    private PlayersEntity playerCreatedWithTheAccount;
    private TeamInvitationsEntity pendingInvitation;
    private PlayerRegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        team = new TeamsEntity();
        team.setId(UUID.randomUUID());

        // Registering the account creates this player, as UsersAuthService does.
        playerCreatedWithTheAccount = new PlayersEntity();
        playerCreatedWithTheAccount.setId(UUID.randomUUID());
        playerCreatedWithTheAccount.setEmail(EMAIL);

        pendingInvitation = new TeamInvitationsEntity();
        pendingInvitation.setToken(TOKEN);
        pendingInvitation.setTeamId(team.getId());
        pendingInvitation.setStatus(InvitationStatus.PENDING.name());
        pendingInvitation.setExpiresAt(LocalDateTime.now().plusDays(1));

        registerRequest = new PlayerRegisterRequest();
        registerRequest.setFirstName("Ana");
        registerRequest.setLastName("Gómez");
        registerRequest.setEmail("  Ana@Email.com ");
        registerRequest.setPassword("password123");
        registerRequest.setPhone("0991234567");
        registerRequest.setInviteToken(TOKEN);
    }

    private void givenTheAccountCreatesItsPlayer() {
        when(playerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(playerCreatedWithTheAccount));
        when(playerRepository.save(playerCreatedWithTheAccount)).thenReturn(playerCreatedWithTheAccount);
    }

    private void givenAPendingInvitationForTheTeam() {
        when(teamInvitationRepository.findByToken(TOKEN)).thenReturn(Optional.of(pendingInvitation));
        when(teamRepository.findActiveById(team.getId())).thenReturn(Optional.of(team));
    }

    // ─── registerWithInvite ─────────────────────────────────────────────────────

    @Test
    void registerWithInvite_whenTheInvitationIsValid_thenCreatesTheAccountWithTheChosenPassword() {
        givenAPendingInvitationForTheTeam();
        givenTheAccountCreatesItsPlayer();

        playersService.registerWithInvite(registerRequest);

        ArgumentCaptor<RegisterRequest> account = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(usersAuthService).register(account.capture());
        assertThat(account.getValue().getEmail()).isEqualTo("  Ana@Email.com ");
        assertThat(account.getValue().getPassword()).isEqualTo("password123");
        assertThat(account.getValue().getFirstName()).isEqualTo("Ana");
        assertThat(account.getValue().getLastName()).isEqualTo("Gómez");
    }

    @Test
    void registerWithInvite_whenTheInvitationIsValid_thenThePlayerJoinsTheTeamAndTheInvitationIsUsed() {
        givenAPendingInvitationForTheTeam();
        givenTheAccountCreatesItsPlayer();

        PlayerResponse response = playersService.registerWithInvite(registerRequest);

        assertThat(playerCreatedWithTheAccount.getTeam()).isSameAs(team);
        assertThat(playerCreatedWithTheAccount.getPhone()).isEqualTo("0991234567");
        assertThat(pendingInvitation.getStatus()).isEqualTo(InvitationStatus.USED.name());
        verify(teamInvitationRepository).save(pendingInvitation);
        assertThat(response.getTeamId()).isEqualTo(team.getId());
    }

    @Test
    void registerWithInvite_whenThereIsNoInvitation_thenCreatesTheAccountAndThePlayerHasNoTeam() {
        registerRequest.setInviteToken(null);
        givenTheAccountCreatesItsPlayer();

        playersService.registerWithInvite(registerRequest);

        verify(usersAuthService).register(any(RegisterRequest.class));
        assertThat(playerCreatedWithTheAccount.getTeam()).isNull();
        verifyNoInteractions(teamInvitationRepository);
    }

    @Test
    void registerWithInvite_whenTheTokenDoesNotExist_thenNoAccountIsCreated() {
        when(teamInvitationRepository.findByToken(TOKEN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playersService.registerWithInvite(registerRequest))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Invalid invitation token");

        verifyNoInteractions(usersAuthService);
    }

    @Test
    void registerWithInvite_whenTheInvitationWasAlreadyUsed_thenNoAccountIsCreated() {
        pendingInvitation.setStatus(InvitationStatus.USED.name());
        when(teamInvitationRepository.findByToken(TOKEN)).thenReturn(Optional.of(pendingInvitation));

        assertThatThrownBy(() -> playersService.registerWithInvite(registerRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been used");

        verifyNoInteractions(usersAuthService);
    }

    @Test
    void registerWithInvite_whenTheInvitationExpired_thenNoAccountIsCreated() {
        pendingInvitation.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(teamInvitationRepository.findByToken(TOKEN)).thenReturn(Optional.of(pendingInvitation));

        assertThatThrownBy(() -> playersService.registerWithInvite(registerRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");

        verifyNoInteractions(usersAuthService);
    }

    @Test
    void registerWithInvite_whenTheEmailAlreadyHasAnAccount_thenTheInvitationStaysPending() {
        givenAPendingInvitationForTheTeam();
        doThrow(new DataIntegrityViolationException("Email already in use"))
                .when(usersAuthService).register(any(RegisterRequest.class));

        assertThatThrownBy(() -> playersService.registerWithInvite(registerRequest))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(pendingInvitation.getStatus()).isEqualTo(InvitationStatus.PENDING.name());
        verify(teamInvitationRepository, never()).save(any());
    }

    // ─── create ─────────────────────────────────────────────────────────────────

    @Test
    void create_whenTheRequestIsValid_thenCreatesTheAccountAndPutsThePlayerInTheTeam() {
        CreatePlayerRequest request = new CreatePlayerRequest();
        request.setFirstName("Ana");
        request.setLastName("Gómez");
        request.setEmail(EMAIL);
        request.setPassword("password123");
        request.setTeamId(team.getId());
        when(teamRepository.findActiveById(team.getId())).thenReturn(Optional.of(team));
        givenTheAccountCreatesItsPlayer();

        playersService.create(request);

        ArgumentCaptor<RegisterRequest> account = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(usersAuthService).register(account.capture());
        assertThat(account.getValue().getEmail()).isEqualTo(EMAIL);
        assertThat(playerCreatedWithTheAccount.getTeam()).isSameAs(team);
    }

    // ─── invitePlayer ───────────────────────────────────────────────────────────

    @Test
    void invitePlayer_whenTheEmailIsWrittenWithCapitals_thenTheInvitationIsForTheEmailOfTheAccount() {
        InvitePlayerRequest request = new InvitePlayerRequest();
        request.setEmail("Ana@EMAIL.com");
        request.setTeamId(team.getId());
        playerCreatedWithTheAccount.setFirstName("Ana");
        playerCreatedWithTheAccount.setLastName("Gómez");
        team.setName("Los Tigres");
        when(playerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(playerCreatedWithTheAccount));
        when(teamRepository.findActiveById(team.getId())).thenReturn(Optional.of(team));

        playersService.invitePlayer(request);

        ArgumentCaptor<TeamInvitationsEntity> invitation = ArgumentCaptor.forClass(TeamInvitationsEntity.class);
        verify(teamInvitationRepository).save(invitation.capture());
        assertThat(invitation.getValue().getEmail()).isEqualTo(EMAIL);
        verify(emailService).sendTeamInvitation(EMAIL, "Ana Gómez", "Los Tigres");
    }
}
