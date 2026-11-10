package com.platform.backend.modules.users.application.services;

import com.platform.backend.modules.users.application.mappers.UserMapper;
import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.domain.exception.AccountSuspensionError;
import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.modules.users.infrastructure.configuration.security.AuthenticatedUser;
import com.platform.backend.modules.users.infrastructure.configuration.security.PlatformOwnerRole;
import com.platform.backend.modules.users.presentation.responses.UserResponse.UserResponse;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountSuspensionServiceTest {

    private static final UUID CALLER_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    @Mock private IUserRepository userRepository;
    @Mock private PlatformOwnerRole platformOwnerRole;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private AccountSuspensionService accountSuspensionService;

    private UsersEntity account;

    @BeforeEach
    void setUp() {
        account = new UsersEntity();
        account.setId(ACCOUNT_ID);
        account.setEmail("ana@email.com");
        lenient().when(userRepository.findActiveById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        lenient().when(userRepository.save(any(UsersEntity.class))).thenAnswer(call -> call.getArgument(0));
        lenient().when(userMapper.toUserResponse(any(UsersEntity.class))).thenReturn(new UserResponse());
        signInAs(false);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void signInAs(boolean platformOwner) {
        AuthenticatedUser caller = AuthenticatedUser.from(
                CALLER_ID, "Admin", null, "Org", null, "admin@email.com", UUID.randomUUID(), true, platformOwner);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(caller, null, caller.getAuthorities()));
    }

    @Test
    void changeSuspension_whenSuspending_thenTheAccountIsMarkedAndItsOpenSessionsEnd() {
        LocalDateTime before = LocalDateTime.now();

        accountSuspensionService.changeSuspension(ACCOUNT_ID, true);

        assertThat(account.isSuspended()).isTrue();
        assertThat(account.getSuspendedAt()).isAfterOrEqualTo(before);
        // The same moment closes the sessions, so reactivating the account does not bring back the tokens it had.
        assertThat(account.getTokensValidAfter()).isEqualTo(account.getSuspendedAt());
        verify(userRepository).save(account);
    }

    @Test
    void changeSuspension_whenAlreadySuspended_thenTheFirstMomentIsKept() {
        LocalDateTime firstSuspension = LocalDateTime.now().minusDays(3);
        account.setSuspendedAt(firstSuspension);
        account.setTokensValidAfter(firstSuspension);

        accountSuspensionService.changeSuspension(ACCOUNT_ID, true);

        assertThat(account.getSuspendedAt()).isEqualTo(firstSuspension);
        assertThat(account.getTokensValidAfter()).isEqualTo(firstSuspension);
    }

    @Test
    void changeSuspension_whenReactivating_thenTheSuspensionIsClearedButTheOldTokensStayDead() {
        LocalDateTime suspendedAt = LocalDateTime.now().minusDays(1);
        account.setSuspendedAt(suspendedAt);
        account.setTokensValidAfter(suspendedAt);

        accountSuspensionService.changeSuspension(ACCOUNT_ID, false);

        assertThat(account.isSuspended()).isFalse();
        assertThat(account.getTokensValidAfter()).isEqualTo(suspendedAt);
    }

    @Test
    void changeSuspension_whenTheCallerTargetsThemselves_thenItIsRefused() {
        account.setId(CALLER_ID);
        when(userRepository.findActiveById(CALLER_ID)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountSuspensionService.changeSuspension(CALLER_ID, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(AccountSuspensionError.CANNOT_SUSPEND_YOURSELF.name());

        verify(userRepository, never()).save(any(UsersEntity.class));
    }

    @Test
    void changeSuspension_whenAnOrganizationAdministratorTargetsAPlatformOwner_thenItIsRefused() {
        when(platformOwnerRole.isOwner(ACCOUNT_ID)).thenReturn(true);

        assertThatThrownBy(() -> accountSuspensionService.changeSuspension(ACCOUNT_ID, true))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage(AccountSuspensionError.PLATFORM_OWNER_PROTECTED.name());

        assertThat(account.isSuspended()).isFalse();
    }

    @Test
    void changeSuspension_whenThePlatformOwnerTargetsAnotherPlatformOwner_thenItWorks() {
        signInAs(true);
        when(platformOwnerRole.isOwner(ACCOUNT_ID)).thenReturn(true);

        accountSuspensionService.changeSuspension(ACCOUNT_ID, true);

        assertThat(account.isSuspended()).isTrue();
    }

    @Test
    void changeSuspension_whenTheAccountIsNotVisibleToTheCaller_thenItIsNotFound() {
        UUID otherOrganizationsAccount = UUID.randomUUID();
        when(userRepository.findActiveById(otherOrganizationsAccount)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountSuspensionService.changeSuspension(otherOrganizationsAccount, true))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
