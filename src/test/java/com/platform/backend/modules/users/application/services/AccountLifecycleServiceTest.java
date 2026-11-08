package com.platform.backend.modules.users.application.services;

import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.irepositories.IPlayerPhotoRepository;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.domain.exception.AccountDeletionError;
import com.platform.backend.modules.users.domain.irepositories.IEmailVerificationTokenRepository;
import com.platform.backend.modules.users.domain.irepositories.IPasswordResetTokenRepository;
import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.modules.users.domain.irepositories.IUsersRolesRepository;
import com.platform.backend.modules.users.infrastructure.configuration.security.PlatformOwnerRole;
import com.platform.backend.modules.users.presentation.requests.DeleteAccountRequest.DeleteAccountRequest;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountLifecycleServiceTest {

    private static final String EMAIL = "ana@email.com";
    private static final UUID ADMIN_ROLE_ID = UUID.randomUUID();

    @Mock private IUserRepository userRepository;
    @Mock private IPlayerRepository playerRepository;
    @Mock private IPlayerPhotoRepository playerPhotoRepository;
    @Mock private IUsersRolesRepository usersRolesRepository;
    @Mock private IPasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private IEmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private PlatformOwnerRole platformOwnerRole;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountLifecycleService accountLifecycleService;

    private UsersEntity user;
    private PlayersEntity player;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(accountLifecycleService, "adminRoleId", ADMIN_ROLE_ID.toString());
        ReflectionTestUtils.setField(accountLifecycleService, "erasedPlayerName", "Cuenta eliminada");

        user = new UsersEntity();
        user.setId(UUID.randomUUID());
        user.setEmail(EMAIL);
        user.setFirstName("Ana");
        user.setLastName("Gómez");
        user.setUsername("ana");
        user.setPassword("hashed");
        user.setEmailVerifiedAt(LocalDateTime.now().minusDays(3));

        TeamsEntity team = new TeamsEntity();
        team.setId(UUID.randomUUID());
        player = new PlayersEntity();
        player.setId(UUID.randomUUID());
        player.setEmail(EMAIL);
        player.setFirstName("Ana");
        player.setLastName("Gómez");
        player.setPhone("0991234567");
        player.setProfilePhotoUrl("https://photos/ana.png");
        player.setTeam(team);
        player.setIsCaptain(true);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    private static DeleteAccountRequest requestWith(String password) {
        DeleteAccountRequest request = new DeleteAccountRequest();
        request.setPassword(password);
        return request;
    }

    // ─── signOut ────────────────────────────────────────────────────────────────

    @Test
    void signOut_whenCalled_thenTokensIssuedUntilNowStopWorking() {
        LocalDateTime before = LocalDateTime.now();

        accountLifecycleService.signOut(EMAIL);

        assertThat(user.getTokensValidAfter()).isAfterOrEqualTo(before);
        verify(userRepository).save(user);
    }

    // ─── deleteAccount ──────────────────────────────────────────────────────────

    @Test
    void deleteAccount_whenThePasswordIsWrong_thenItIsAFieldErrorAndNothingIsErased() {
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> accountLifecycleService.deleteAccount(requestWith("wrong"), EMAIL))
                .isInstanceOfSatisfying(FieldValidationException.class, exception ->
                        assertThat(exception.getFieldErrors())
                                .containsOnly(entry("password", AccountDeletionError.INCORRECT_PASSWORD.name())));

        assertThat(user.getDeletedAt()).isNull();
        assertThat(user.getEmail()).isEqualTo(EMAIL);
        verify(userRepository, never()).save(any());
        verify(playerRepository, never()).save(any());
    }

    @Test
    void deleteAccount_whenTheAccountIsAnAdministrator_thenItIsRefused() {
        when(usersRolesRepository.existsActiveByUserAndRole(user.getId(), ADMIN_ROLE_ID)).thenReturn(true);

        assertThatThrownBy(() -> accountLifecycleService.deleteAccount(requestWith("secret"), EMAIL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(AccountDeletionError.STAFF_ACCOUNT.name());

        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteAccount_whenTheAccountIsThePlatformOwner_thenItIsRefused() {
        when(platformOwnerRole.isOwner(user.getId())).thenReturn(true);

        assertThatThrownBy(() -> accountLifecycleService.deleteAccount(requestWith("secret"), EMAIL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(AccountDeletionError.STAFF_ACCOUNT.name());
    }

    private void givenThePasswordIsRight() {
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);
        when(passwordEncoder.encode(any())).thenReturn("unusable-hash");
        when(playerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(player));
    }

    @Test
    void deleteAccount_whenThePasswordIsRight_thenThePersonalDataOfTheAccountIsErased() {
        givenThePasswordIsRight();

        accountLifecycleService.deleteAccount(requestWith("secret"), EMAIL);

        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.getFirstName()).isNull();
        assertThat(user.getLastName()).isNull();
        assertThat(user.getUsername()).isNull();
        assertThat(user.getEmail()).isEqualTo("deleted-" + user.getId() + "@deleted.invalid").doesNotContain("ana");
        assertThat(user.getPassword()).isEqualTo("unusable-hash");
        assertThat(user.isEmailVerified()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void deleteAccount_whenThePasswordIsRight_thenTheSessionsOfTheAccountStopWorkingAtOnce() {
        givenThePasswordIsRight();
        LocalDateTime before = LocalDateTime.now();

        accountLifecycleService.deleteAccount(requestWith("secret"), EMAIL);

        assertThat(user.getTokensValidAfter()).isAfterOrEqualTo(before);
    }

    @Test
    void deleteAccount_whenThePlayerHadATeamAndWasItsCaptain_thenThePlayerLeavesItAndIsErasedToo() {
        givenThePasswordIsRight();

        accountLifecycleService.deleteAccount(requestWith("secret"), EMAIL);

        assertThat(player.getTeam()).isNull();
        assertThat(player.getIsCaptain()).isFalse();
        assertThat(player.getFirstName()).isEqualTo("Cuenta eliminada");
        assertThat(player.getLastName()).isEmpty();
        assertThat(player.getEmail()).isEqualTo("deleted-" + player.getId() + "@deleted.invalid");
        assertThat(player.getPhone()).isNull();
        assertThat(player.getProfilePhotoUrl()).isNull();
        assertThat(player.getDeletedAt()).isNotNull();
        verify(playerRepository).save(player);
    }

    @Test
    void deleteAccount_whenThePlayerHadAPhoto_thenItsBytesAreErased() {
        givenThePasswordIsRight();

        accountLifecycleService.deleteAccount(requestWith("secret"), EMAIL);

        verify(playerPhotoRepository).deleteByPlayerId(player.getId());
    }

    @Test
    void deleteAccount_whenThePasswordIsRight_thenThePendingEmailLinksAreRemoved() {
        givenThePasswordIsRight();

        accountLifecycleService.deleteAccount(requestWith("secret"), EMAIL);

        verify(passwordResetTokenRepository).deleteByUserId(user.getId());
        verify(emailVerificationTokenRepository).deleteByUserId(user.getId());
    }

    @Test
    void deleteAccount_whenTheAccountHasNoPlayer_thenTheAccountIsStillErased() {
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);
        when(passwordEncoder.encode(any())).thenReturn("unusable-hash");
        when(playerRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        accountLifecycleService.deleteAccount(requestWith("secret"), EMAIL);

        assertThat(user.getDeletedAt()).isNotNull();
        verify(playerRepository, never()).save(any());
        verify(playerPhotoRepository, never()).deleteByPlayerId(any());
    }
}
