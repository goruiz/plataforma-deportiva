package com.platform.backend.modules.users.application.services;

import com.platform.backend.modules.organizations.domain.irepositories.IOrganizationRepository;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.users.application.iservices.IAccountCapabilitiesService;
import com.platform.backend.modules.users.application.iservices.IEmailVerificationService;
import com.platform.backend.modules.users.application.mappers.UserMapper;
import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.domain.enums.AccountCapability;
import com.platform.backend.modules.users.domain.irepositories.IPasswordResetTokenRepository;
import com.platform.backend.modules.users.domain.irepositories.IRoleRepository;
import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.modules.users.domain.irepositories.IUsersRolesRepository;
import com.platform.backend.modules.users.infrastructure.configuration.security.PlatformOwnerRole;
import com.platform.backend.modules.users.presentation.requests.LoginRequest.ClientType;
import com.platform.backend.modules.users.presentation.requests.LoginRequest.LoginRequest;
import com.platform.backend.modules.users.presentation.requests.RegisterRequest.RegisterRequest;
import com.platform.backend.modules.users.presentation.responses.AuthResponse.AuthResponse;
import com.platform.backend.modules.users.presentation.responses.RegisterResponse.RegisterResponse;
import com.platform.backend.modules.users.presentation.responses.UserResponse.UserResponse;
import com.platform.backend.shared.infraestructure.email.EmailDeliveryException;
import com.platform.backend.shared.infraestructure.config.PersonalDataValidator;
import com.platform.backend.shared.infraestructure.email.EmailService;
import com.platform.backend.shared.infraestructure.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** What the account does with its email: confirm it after an open registration, or already have it proved. */
@ExtendWith(MockitoExtension.class)
class UsersServiceVerificationTest {

    private static final String EMAIL = "juan@email.com";

    @Mock private IUserRepository userRepository;
    @Mock private IPlayerRepository playerRepository;
    @Mock private IPasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private IUsersRolesRepository usersRolesRepository;
    @Mock private IRoleRepository roleRepository;
    @Mock private IOrganizationRepository organizationRepository;
    @Mock private PlatformOwnerRole platformOwnerRole;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private EmailService emailService;
    @Mock private IEmailVerificationService emailVerificationService;
    @Mock private PersonalDataValidator personalDataValidator;
    @Mock private IAccountCapabilitiesService accountCapabilitiesService;

    @InjectMocks
    private UsersAuthService usersAuthService;

    private RegisterRequest registerRequest;
    private UsersEntity newAccount;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail(EMAIL);
        registerRequest.setPassword("password123");

        newAccount = new UsersEntity();
        newAccount.setId(UUID.randomUUID());
        newAccount.setEmail(EMAIL);
    }

    private void givenTheAccountIsSaved() {
        when(userMapper.toEntity(registerRequest)).thenReturn(newAccount);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(newAccount)).thenReturn(newAccount);
        when(jwtService.generateToken(newAccount, EMAIL)).thenReturn("jwt-token");
    }

    // ─── register ───────────────────────────────────────────────────────────────

    @Test
    void register_whenOpenRegistration_thenTheEmailStartsUnconfirmedAndALinkIsSent() {
        givenTheAccountIsSaved();

        RegisterResponse response = usersAuthService.register(registerRequest);

        assertThat(newAccount.isEmailVerified()).isFalse();
        assertThat(response.isEmailVerified()).isFalse();
        verify(emailVerificationService).sendVerificationLink(newAccount);
    }

    @Test
    void register_whenTheLinkCannotBeEmailed_thenTheRegistrationStillStands() {
        givenTheAccountIsSaved();
        doThrow(new EmailDeliveryException("smtp down", new RuntimeException()))
                .when(emailVerificationService).sendVerificationLink(newAccount);

        RegisterResponse response = usersAuthService.register(registerRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        verify(userRepository).save(newAccount);
    }

    // ─── registerVerified ───────────────────────────────────────────────────────

    @Test
    void registerVerified_whenAnInvitationVouchedForTheEmail_thenTheEmailIsConfirmedAndNoLinkIsSent() {
        givenTheAccountIsSaved();

        RegisterResponse response = usersAuthService.registerVerified(registerRequest);

        assertThat(newAccount.isEmailVerified()).isTrue();
        assertThat(response.isEmailVerified()).isTrue();
        verify(emailVerificationService, never()).sendVerificationLink(any());
    }

    // ─── login ──────────────────────────────────────────────────────────────────

    @Test
    void login_whenTheEmailIsNotConfirmed_thenTheAnswerSaysSo() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(EMAIL);
        loginRequest.setPassword("password123");
        loginRequest.setClientType(ClientType.MOBILE_APP);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(newAccount));
        when(jwtService.generateToken(anyMap(), eq(EMAIL))).thenReturn("jwt-token");

        assertThat(usersAuthService.login(loginRequest).isEmailVerified()).isFalse();

        newAccount.setEmailVerifiedAt(LocalDateTime.now());
        AuthResponse confirmed = usersAuthService.login(loginRequest);
        assertThat(confirmed.isEmailVerified()).isTrue();
    }

    // ─── getCurrentAccount ──────────────────────────────────────────────────────

    @Test
    void getCurrentAccount_whenTheAccountExists_thenItIsReadAsItIsNow() {
        UserResponse mapped = new UserResponse();
        mapped.setEmailVerified(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(newAccount));
        when(userMapper.toUserResponse(newAccount)).thenReturn(mapped);

        assertThat(usersAuthService.getCurrentAccount("  Juan@Email.com ").isEmailVerified()).isTrue();
    }

    @Test
    void getCurrentAccount_saysWhatTheAccountMayDoBeyondTheBasics() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(newAccount));
        when(userMapper.toUserResponse(newAccount)).thenReturn(new UserResponse());
        when(accountCapabilitiesService.of(newAccount.getId())).thenReturn(Set.of(AccountCapability.OPEN_MATCH_DISPUTES));

        assertThat(usersAuthService.getCurrentAccount(EMAIL).getCapabilities()).containsExactly("OPEN_MATCH_DISPUTES");
    }

    @Test
    void getCurrentAccount_anOrdinaryAccountHasNoCapabilities() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(newAccount));
        when(userMapper.toUserResponse(newAccount)).thenReturn(new UserResponse());
        when(accountCapabilitiesService.of(newAccount.getId())).thenReturn(Set.of());

        assertThat(usersAuthService.getCurrentAccount(EMAIL).getCapabilities()).isEmpty();
    }

    @Test
    void getCurrentAccount_whenTheAccountWasDeleted_thenItIsAnUnknownAccount() {
        newAccount.setDeletedAt(LocalDateTime.now());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(newAccount));

        assertThatThrownBy(() -> usersAuthService.getCurrentAccount(EMAIL))
                .isInstanceOf(BadCredentialsException.class);
    }
}
