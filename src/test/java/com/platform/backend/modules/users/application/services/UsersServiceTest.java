package com.platform.backend.modules.users.application.services;

import com.platform.backend.modules.organizations.domain.entities.OrganizationsEntity;
import com.platform.backend.modules.organizations.domain.enums.OrganizationStatus;
import com.platform.backend.modules.organizations.domain.irepositories.IOrganizationRepository;
import com.platform.backend.modules.users.application.mappers.UserMapper;
import com.platform.backend.modules.users.domain.entities.PasswordResetTokenEntity;
import com.platform.backend.modules.users.domain.entities.RolesEntity;
import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.domain.entities.UsersRolesEntity;
import com.platform.backend.modules.users.domain.irepositories.IPasswordResetTokenRepository;
import com.platform.backend.modules.users.domain.irepositories.IRoleRepository;
import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.modules.users.domain.exception.LoginError;
import com.platform.backend.modules.users.domain.irepositories.IUsersRolesRepository;
import com.platform.backend.modules.users.infrastructure.configuration.security.PlatformOwnerRole;
import com.platform.backend.modules.users.presentation.requests.ChangePasswordRequest.ChangePasswordRequest;
import com.platform.backend.modules.users.presentation.requests.ForgotPasswordRequest.ForgotPasswordRequest;
import com.platform.backend.modules.users.presentation.requests.LoginRequest.ClientType;
import com.platform.backend.modules.users.presentation.requests.LoginRequest.LoginRequest;
import com.platform.backend.modules.users.presentation.requests.RegisterRequest.RegisterRequest;
import com.platform.backend.modules.users.presentation.requests.ResetPasswordRequest.ResetPasswordRequest;
import com.platform.backend.modules.users.presentation.responses.AuthResponse.AuthResponse;
import com.platform.backend.modules.users.presentation.responses.RegisterResponse.RegisterResponse;
import com.platform.backend.shared.infraestructure.email.EmailService;
import com.platform.backend.shared.infraestructure.email.EmailDeliveryException;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import com.platform.backend.shared.infraestructure.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsersServiceTest {

    @Mock private IUserRepository userRepository;
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

    @InjectMocks
    private UsersAuthService usersAuthService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private UsersEntity usersEntity;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("juan@email.com");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("juan@email.com");
        loginRequest.setPassword("password123");
        // No admin role is configured in this test, so the user is never an admin
        // and the mobile client type is the one allowed through.
        loginRequest.setClientType(ClientType.MOBILE_APP);

        usersEntity = new UsersEntity();
        usersEntity.setId(UUID.randomUUID());
        usersEntity.setEmail("juan@email.com");
        usersEntity.setUsername("jperez");
        usersEntity.setPassword("hashed_password");
        ReflectionTestUtils.setField(usersAuthService, "passwordResetExpirationMinutes", 30L);
    }

    // ─── register ───────────────────────────────────────────────────────────────

    @Test
    void register_whenValidRequest_thenReturnTokenInResponse() {
        when(userMapper.toEntity(registerRequest)).thenReturn(usersEntity);
        when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
        when(userRepository.save(usersEntity)).thenReturn(usersEntity);
        when(jwtService.generateToken(usersEntity, "juan@email.com")).thenReturn("jwt-token");

        RegisterResponse response = usersAuthService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        verify(userRepository).save(usersEntity);
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void register_whenSaved_thenPasswordIsEncoded() {
        when(userMapper.toEntity(registerRequest)).thenReturn(usersEntity);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(userRepository.save(any())).thenReturn(usersEntity);
        when(jwtService.generateToken(any(UsersEntity.class), anyString())).thenReturn("token");

        usersAuthService.register(registerRequest);

        verify(passwordEncoder).encode("password123");
    }

    @Test
    void register_whenMobileUserRoleIsConfigured_thenAssignsItToTheNewAccount() {
        UUID mobileUserRoleId = UUID.randomUUID();
        RolesEntity mobileUserRole = new RolesEntity();
        mobileUserRole.setId(mobileUserRoleId);
        ReflectionTestUtils.setField(usersAuthService, "mobileUserRoleId", mobileUserRoleId.toString());
        when(userMapper.toEntity(registerRequest)).thenReturn(usersEntity);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(userRepository.save(any())).thenReturn(usersEntity);
        when(roleRepository.findActiveById(mobileUserRoleId)).thenReturn(Optional.of(mobileUserRole));
        when(jwtService.generateToken(any(UsersEntity.class), anyString())).thenReturn("token");

        usersAuthService.register(registerRequest);

        ArgumentCaptor<UsersRolesEntity> assignment = ArgumentCaptor.forClass(UsersRolesEntity.class);
        verify(usersRolesRepository).save(assignment.capture());
        assertThat(assignment.getValue().getUser()).isSameAs(usersEntity);
        assertThat(assignment.getValue().getRole()).isSameAs(mobileUserRole);
    }

    @Test
    void register_whenNoMobileUserRoleIsConfigured_thenRegistersWithoutRole() {
        when(userMapper.toEntity(registerRequest)).thenReturn(usersEntity);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(userRepository.save(any())).thenReturn(usersEntity);
        when(jwtService.generateToken(any(UsersEntity.class), anyString())).thenReturn("token");

        usersAuthService.register(registerRequest);

        verifyNoInteractions(roleRepository);
        verify(usersRolesRepository, never()).save(any());
    }

    @Test
    void register_whenTheConfiguredRoleDoesNotExist_thenStillRegistersTheAccount() {
        UUID missingRoleId = UUID.randomUUID();
        ReflectionTestUtils.setField(usersAuthService, "mobileUserRoleId", missingRoleId.toString());
        when(userMapper.toEntity(registerRequest)).thenReturn(usersEntity);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(userRepository.save(any())).thenReturn(usersEntity);
        when(roleRepository.findActiveById(missingRoleId)).thenReturn(Optional.empty());
        when(jwtService.generateToken(any(UsersEntity.class), anyString())).thenReturn("token");

        RegisterResponse response = usersAuthService.register(registerRequest);

        assertThat(response.getToken()).isEqualTo("token");
        verify(usersRolesRepository, never()).save(any());
    }

    // ─── login ──────────────────────────────────────────────────────────────────

    @Test
    void login_whenValidCredentials_thenReturnToken() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("juan@email.com", "password123"));
        when(userRepository.findByEmail("juan@email.com")).thenReturn(Optional.of(usersEntity));
        when(jwtService.generateToken(anyMap(), eq("juan@email.com"))).thenReturn("jwt-login-token");

        AuthResponse response = usersAuthService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-login-token");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmail("juan@email.com");
    }

    @Test
    void login_whenInvalidCredentials_thenPropagatesBadCredentialsException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> usersAuthService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void login_whenAuthPassesButUserNotFound_thenThrowsBadCredentialsException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("juan@email.com", "password123"));
        when(userRepository.findByEmail("juan@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usersAuthService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");
    }

    // ─── login: the platform owner and the organizations ────────────────────────

    private static final UUID ADMIN_ROLE_ID = UUID.randomUUID();
    private static final UUID ORGANIZATION_ID = UUID.randomUUID();

    private void givenAnAdministratorWhoLogsInThroughThePanel() {
        ReflectionTestUtils.setField(usersAuthService, "adminRoleId", ADMIN_ROLE_ID.toString());
        loginRequest.setClientType(ClientType.ADMIN_PANEL);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("juan@email.com", "password123"));
        when(userRepository.findByEmail("juan@email.com")).thenReturn(Optional.of(usersEntity));
        when(usersRolesRepository.existsActiveByUserAndRole(usersEntity.getId(), ADMIN_ROLE_ID)).thenReturn(true);
    }

    private OrganizationsEntity organization(OrganizationStatus status) {
        OrganizationsEntity organization = new OrganizationsEntity();
        organization.setId(ORGANIZATION_ID);
        organization.setStatus(status.name());
        return organization;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturedTokenClaims() {
        ArgumentCaptor<Map<String, Object>> claims = ArgumentCaptor.forClass(Map.class);
        verify(jwtService).generateToken(claims.capture(), eq("juan@email.com"));
        return claims.getValue();
    }

    private void givenTheOwnerWhoHoldsNoAdministratorRole(ClientType clientType) {
        ReflectionTestUtils.setField(usersAuthService, "adminRoleId", ADMIN_ROLE_ID.toString());
        loginRequest.setClientType(clientType);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("juan@email.com", "password123"));
        when(userRepository.findByEmail("juan@email.com")).thenReturn(Optional.of(usersEntity));
        when(platformOwnerRole.isOwner(usersEntity.getId())).thenReturn(true);
    }

    @Test
    void login_whenThePlatformOwnerEntersThePanel_thenTheTokenSaysSoAndHasNoOrganization() {
        givenTheOwnerWhoHoldsNoAdministratorRole(ClientType.ADMIN_PANEL);
        when(jwtService.generateToken(anyMap(), eq("juan@email.com"))).thenReturn("owner-token");

        AuthResponse response = usersAuthService.login(loginRequest);

        assertThat(response.isPlatformOwner()).isTrue();
        assertThat(response.getOrganizationId()).isNull();
        assertThat(capturedTokenClaims()).containsEntry("platformOwner", true).containsEntry("admin", true);
        verifyNoInteractions(organizationRepository);
        // The owner has its own role: it counts as an administrator without the customers' administrator role.
        verify(usersRolesRepository, never()).existsActiveByUserAndRole(any(), any());
    }

    @Test
    void login_whenThePlatformOwnerTriesTheMobileApp_thenAccessIsDenied() {
        givenTheOwnerWhoHoldsNoAdministratorRole(ClientType.MOBILE_APP);

        assertThatThrownBy(() -> usersAuthService.login(loginRequest))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage(LoginError.ADMIN_CANNOT_USE_MOBILE_APP.name());

        verify(jwtService, never()).generateToken(anyMap(), anyString());
    }

    @Test
    void login_whenAnOrganizationAdministratorEntersThePanel_thenTheTokenCarriesTheirOrganization() {
        givenAnAdministratorWhoLogsInThroughThePanel();
        usersEntity.setOrganizationId(ORGANIZATION_ID);
        when(organizationRepository.findActiveById(ORGANIZATION_ID)).thenReturn(Optional.of(organization(OrganizationStatus.ACTIVE)));
        when(jwtService.generateToken(anyMap(), eq("juan@email.com"))).thenReturn("organization-token");

        AuthResponse response = usersAuthService.login(loginRequest);

        assertThat(response.isPlatformOwner()).isFalse();
        assertThat(response.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
        assertThat(capturedTokenClaims())
                .containsEntry("platformOwner", false)
                .containsEntry("organizationId", ORGANIZATION_ID.toString());
    }

    @Test
    void login_whenAnAdministratorBelongsToNoOrganizationAndIsNotTheOwner_thenAccessIsDenied() {
        givenAnAdministratorWhoLogsInThroughThePanel();

        assertThatThrownBy(() -> usersAuthService.login(loginRequest))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage(LoginError.ORGANIZATION_REQUIRED.name());

        verify(jwtService, never()).generateToken(anyMap(), anyString());
    }

    @Test
    void login_whenTheOrganizationIsSuspended_thenItsAdministratorsAreDenied() {
        givenAnAdministratorWhoLogsInThroughThePanel();
        usersEntity.setOrganizationId(ORGANIZATION_ID);
        when(organizationRepository.findActiveById(ORGANIZATION_ID)).thenReturn(Optional.of(organization(OrganizationStatus.SUSPENDED)));

        assertThatThrownBy(() -> usersAuthService.login(loginRequest))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage(LoginError.ORGANIZATION_SUSPENDED.name());

        verify(jwtService, never()).generateToken(anyMap(), anyString());
    }

    @Test
    void login_whenTheOrganizationWasDeleted_thenItsAdministratorsAreDenied() {
        givenAnAdministratorWhoLogsInThroughThePanel();
        usersEntity.setOrganizationId(ORGANIZATION_ID);
        when(organizationRepository.findActiveById(ORGANIZATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usersAuthService.login(loginRequest))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage(LoginError.ORGANIZATION_NOT_FOUND.name());
    }

    @Test
    void login_whenAMobileAccountBelongsToASuspendedOrganization_thenItIsDeniedToo() {
        usersEntity.setOrganizationId(ORGANIZATION_ID);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("juan@email.com", "password123"));
        when(userRepository.findByEmail("juan@email.com")).thenReturn(Optional.of(usersEntity));
        when(organizationRepository.findActiveById(ORGANIZATION_ID)).thenReturn(Optional.of(organization(OrganizationStatus.SUSPENDED)));

        assertThatThrownBy(() -> usersAuthService.login(loginRequest))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage(LoginError.ORGANIZATION_SUSPENDED.name());
    }

    @Test
    void login_whenAnAccountWithoutAdministratorRoleTriesThePanel_thenAccessIsDenied() {
        ReflectionTestUtils.setField(usersAuthService, "adminRoleId", ADMIN_ROLE_ID.toString());
        loginRequest.setClientType(ClientType.ADMIN_PANEL);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("juan@email.com", "password123"));
        when(userRepository.findByEmail("juan@email.com")).thenReturn(Optional.of(usersEntity));

        assertThatThrownBy(() -> usersAuthService.login(loginRequest))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage(LoginError.ADMIN_ACCESS_REQUIRED.name());

        verify(jwtService, never()).generateToken(anyMap(), anyString());
    }

    // ─── changePassword ─────────────────────────────────────────────────────────

    @Test
    void changePassword_whenCurrentPasswordMatches_thenStoreEncodedNewPassword() {
        when(userRepository.findByEmail("juan@email.com")).thenReturn(Optional.of(usersEntity));
        when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);
        when(passwordEncoder.matches("brand-new-pass", "hashed_password")).thenReturn(false);
        when(passwordEncoder.encode("brand-new-pass")).thenReturn("hashed_new_password");

        usersAuthService.changePassword(buildChangePasswordRequest("password123", "brand-new-pass"), "juan@email.com");

        assertThat(usersEntity.getPassword()).isEqualTo("hashed_new_password");
        verify(userRepository).save(usersEntity);
    }

    @Test
    void changePassword_whenCurrentPasswordIsWrong_thenRejectCurrentPasswordFieldWithoutSaving() {
        when(userRepository.findByEmail("juan@email.com")).thenReturn(Optional.of(usersEntity));
        when(passwordEncoder.matches("wrong-password", "hashed_password")).thenReturn(false);

        assertThatThrownBy(() -> usersAuthService.changePassword(
                buildChangePasswordRequest("wrong-password", "brand-new-pass"), "juan@email.com"))
                .isInstanceOfSatisfying(FieldValidationException.class, exception ->
                        assertThat(exception.getFieldErrors())
                                .containsOnly(entry("currentPassword", "INCORRECT_CURRENT_PASSWORD")));

        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_whenNewPasswordEqualsCurrent_thenRejectNewPasswordFieldWithoutSaving() {
        when(userRepository.findByEmail("juan@email.com")).thenReturn(Optional.of(usersEntity));
        when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);

        assertThatThrownBy(() -> usersAuthService.changePassword(
                buildChangePasswordRequest("password123", "password123"), "juan@email.com"))
                .isInstanceOfSatisfying(FieldValidationException.class, exception ->
                        assertThat(exception.getFieldErrors())
                                .containsOnly(entry("newPassword", "NEW_PASSWORD_SAME_AS_CURRENT")));

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void changePassword_whenUserNoLongerExists_thenThrowsBadCredentialsException() {
        when(userRepository.findByEmail("juan@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usersAuthService.changePassword(
                buildChangePasswordRequest("password123", "brand-new-pass"), "juan@email.com"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void requestPasswordReset_whenAccountDoesNotExist_thenKeepResponseGeneric() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(" unknown@email.com ");
        when(userRepository.findByEmail("unknown@email.com")).thenReturn(Optional.empty());

        usersAuthService.requestPasswordReset(request);

        verify(emailService, never()).sendPasswordReset(anyString(), any(), anyString());
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void requestPasswordReset_whenAccountExists_thenStoreTokenHashAndEmailRawToken()
            throws NoSuchAlgorithmException {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(" JUAN@EMAIL.COM ");
        usersEntity.setFirstName("Juan");
        when(userRepository.findByEmail("juan@email.com")).thenReturn(Optional.of(usersEntity));
        ArgumentCaptor<PasswordResetTokenEntity> tokenCaptor =
                ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
        ArgumentCaptor<String> rawTokenCaptor = ArgumentCaptor.forClass(String.class);

        usersAuthService.requestPasswordReset(request);

        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        verify(emailService).sendPasswordReset(
                eq("juan@email.com"), eq("Juan"), rawTokenCaptor.capture());
        PasswordResetTokenEntity savedToken = tokenCaptor.getValue();
        String rawToken = rawTokenCaptor.getValue();
        String expectedHash = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8)));

        assertThat(rawToken).hasSize(43);
        assertThat(savedToken.getTokenHash()).isEqualTo(expectedHash);
        assertThat(savedToken.getTokenHash()).isNotEqualTo(rawToken);
        assertThat(savedToken.getUserId()).isEqualTo(usersEntity.getId());
        assertThat(savedToken.getExpiresAt())
                .isAfter(LocalDateTime.now())
                .isBefore(LocalDateTime.now().plusMinutes(31));
    }

    @Test
    void requestPasswordReset_whenEmailDeliveryFails_thenKeepResponseGeneric() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("juan@email.com");
        when(userRepository.findByEmail("juan@email.com")).thenReturn(Optional.of(usersEntity));
        doThrow(new EmailDeliveryException("Could not send email", new RuntimeException()))
                .when(emailService)
                .sendPasswordReset(eq("juan@email.com"), any(), anyString());

        usersAuthService.requestPasswordReset(request);

        verify(passwordResetTokenRepository).save(any(PasswordResetTokenEntity.class));
    }

    @Test
    void resetPassword_whenTokenIsValid_thenChangePasswordAndConsumeToken()
            throws NoSuchAlgorithmException {
        String rawToken = "single-use-reset-token";
        String tokenHash = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(rawToken);
        request.setNewPassword("newPassword123");
        PasswordResetTokenEntity resetToken = new PasswordResetTokenEntity();
        resetToken.setId(UUID.randomUUID());
        resetToken.setUserId(usersEntity.getId());
        resetToken.setTokenHash(tokenHash);
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        when(passwordResetTokenRepository.findByTokenHashAndUnusedForUpdate(tokenHash))
                .thenReturn(Optional.of(resetToken));
        when(userRepository.findActiveById(usersEntity.getId())).thenReturn(Optional.of(usersEntity));
        when(passwordEncoder.encode("newPassword123")).thenReturn("hashed_new_password");

        usersAuthService.resetPassword(request);

        assertThat(usersEntity.getPassword()).isEqualTo("hashed_new_password");
        assertThat(resetToken.getUsedAt()).isNotNull();
        verify(userRepository).save(usersEntity);
        verify(passwordResetTokenRepository).save(resetToken);
        verify(passwordResetTokenRepository).deleteByUserIdAndIdNot(
                usersEntity.getId(), resetToken.getId());
    }

    @Test
    void resetPassword_whenTokenIsUnknown_thenRejectWithoutChangingPassword() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("unknown-token");
        request.setNewPassword("newPassword123");
        when(passwordResetTokenRepository.findByTokenHashAndUnusedForUpdate(anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> usersAuthService.resetPassword(request))
                .isInstanceOfSatisfying(FieldValidationException.class, exception ->
                        assertThat(exception.getFieldErrors())
                                .containsOnly(entry("token", "INVALID_OR_EXPIRED")));
        assertThat(usersEntity.getPassword()).isEqualTo("hashed_password");
        verify(userRepository, never()).save(any());
    }

    private ChangePasswordRequest buildChangePasswordRequest(String currentPassword, String newPassword) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword(currentPassword);
        request.setNewPassword(newPassword);
        return request;
    }
}
