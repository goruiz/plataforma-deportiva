package com.platform.backend.modules.users.application.services;

import com.platform.backend.modules.organizations.domain.entities.OrganizationsEntity;
import com.platform.backend.modules.organizations.domain.irepositories.IOrganizationRepository;
import com.platform.backend.modules.users.application.iservices.IUsersAuthService;
import com.platform.backend.modules.users.application.mappers.UserMapper;
import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.domain.entities.UsersRolesEntity;
import com.platform.backend.modules.users.domain.entities.PasswordResetTokenEntity;
import com.platform.backend.modules.users.domain.irepositories.IRoleRepository;
import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.modules.users.domain.irepositories.IPasswordResetTokenRepository;
import com.platform.backend.modules.users.domain.irepositories.IUsersRolesRepository;
import com.platform.backend.modules.users.domain.exception.LoginError;
import com.platform.backend.modules.users.domain.exception.PasswordChangeError;
import com.platform.backend.modules.users.domain.exception.PasswordResetError;
import com.platform.backend.modules.users.infrastructure.configuration.security.PlatformOwnerRole;
import com.platform.backend.modules.users.presentation.requests.ChangePasswordRequest.ChangePasswordRequest;
import com.platform.backend.modules.users.presentation.requests.ForgotPasswordRequest.ForgotPasswordRequest;
import com.platform.backend.modules.users.presentation.requests.LoginRequest.ClientType;
import com.platform.backend.modules.users.presentation.requests.LoginRequest.LoginRequest;
import com.platform.backend.modules.users.presentation.requests.RegisterRequest.RegisterRequest;
import com.platform.backend.modules.users.presentation.requests.ResetPasswordRequest.ResetPasswordRequest;
import com.platform.backend.modules.users.presentation.requests.UpdateProfileRequest.UpdateProfileRequest;
import com.platform.backend.modules.users.presentation.responses.AuthResponse.AuthResponse;
import com.platform.backend.modules.users.presentation.responses.RegisterResponse.RegisterResponse;
import com.platform.backend.modules.users.presentation.responses.UpdateProfileResponse.UpdateProfileResponse;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import com.platform.backend.shared.infraestructure.email.EmailService;
import com.platform.backend.shared.infraestructure.email.EmailDeliveryException;
import com.platform.backend.shared.infraestructure.security.jwt.JwtClaims;
import com.platform.backend.shared.infraestructure.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsersAuthService implements IUsersAuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsersAuthService.class);
    private final IUserRepository userRepository;
    private final IUsersRolesRepository usersRolesRepository;
    private final IRoleRepository roleRepository;
    private final IOrganizationRepository organizationRepository;
    private final PlatformOwnerRole platformOwnerRole;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final IPasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${app.security.password-reset-expiration-minutes:30}")
    private long passwordResetExpirationMinutes;

    @Value("${app.security.admin-role-id:}")
    private String adminRoleId;

    @Value("${app.security.mobile-user-role-id:}")
    private String mobileUserRoleId;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new DataIntegrityViolationException("Email already in use");
        }
        UsersEntity newUser = userMapper.toEntity(registerRequest);
        newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        UsersEntity savedUser = userRepository.save(newUser);
        assignMobileUserRole(savedUser);
        String token = jwtService.generateToken(savedUser, savedUser.getEmail());
        return new RegisterResponse(token);
    }

    /**
     * The menu is granted by role and an account with none sees it empty, so everyone who registers is a mobile-app user
     * (app.security.mobile-user-role-id, seeded by V13 with the app's menu).
     */
    private void assignMobileUserRole(UsersEntity user) {
        if (!StringUtils.hasText(mobileUserRoleId)) {
            return;
        }
        UUID roleId = UUID.fromString(mobileUserRoleId);
        roleRepository.findActiveById(roleId).ifPresentOrElse(role -> {
            UsersRolesEntity assignment = new UsersRolesEntity();
            assignment.setUser(user);
            assignment.setRole(role);
            usersRolesRepository.save(assignment);
        }, () -> LOGGER.warn("The mobile user role {} does not exist: {} registers without a role", roleId, user.getEmail()));
    }

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        String email = loginRequest.getEmail().trim().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, loginRequest.getPassword())
        );

        UsersEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        boolean isPlatformOwner = platformOwnerRole.isOwner(user.getId());
        boolean isAdmin = isPlatformOwner || hasAdminRole(user);
        enforceClientTypeAccess(loginRequest.getClientType(), isAdmin);
        enforceOrganizationAccess(loginRequest.getClientType(), user, isPlatformOwner);

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId().toString());
        claims.put("firstName", user.getFirstName());
        claims.put("middleName", user.getMiddleName());
        claims.put("lastName", user.getLastName());
        claims.put("secondLastName", user.getSecondLastName());
        claims.put(JwtClaims.ADMIN, isAdmin);
        claims.put(JwtClaims.PLATFORM_OWNER, isPlatformOwner);
        claims.put(JwtClaims.ORGANIZATION_ID, user.getOrganizationId() == null ? null : user.getOrganizationId().toString());
        claims.put("clientType", loginRequest.getClientType().name());

        String token = jwtService.generateToken(claims, user.getEmail());
        return new AuthResponse(
                token,
                isAdmin,
                isPlatformOwner,
                user.getOrganizationId(),
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getMiddleName(),
                user.getLastName(),
                user.getSecondLastName()
        );
    }

    /**
     * An administrator is a user with an active users_roles row pointing at the fixed admin role id configured in
     * app.security.admin-role-id — never by role name, since role names are editable, translatable data. The platform
     * owner has its own role instead and counts as an administrator without this one (see {@link #login}).
     */
    private boolean hasAdminRole(UsersEntity user) {
        if (!StringUtils.hasText(adminRoleId)) {
            return false;
        }
        return usersRolesRepository.existsActiveByUserAndRole(user.getId(), UUID.fromString(adminRoleId));
    }

    /**
     * The admin panel and the end-user mobile app share this same login endpoint,
     * so the declared clientType is what keeps each population in its own app:
     * only admins may enter through the admin panel, and admins never authenticate
     * through the mobile app.
     */
    private void enforceClientTypeAccess(ClientType clientType, boolean isAdmin) {
        if (clientType == ClientType.ADMIN_PANEL && !isAdmin) {
            throw new AccessDeniedException(LoginError.ADMIN_ACCESS_REQUIRED.name());
        }
        if (clientType == ClientType.MOBILE_APP && isAdmin) {
            throw new AccessDeniedException(LoginError.ADMIN_CANNOT_USE_MOBILE_APP.name());
        }
    }

    /**
     * Whoever is not the platform owner works for an organization, and that organization has to exist and not be
     * suspended: an administrator can only enter the panel through one, and a mobile-app account that belongs to one
     * is cut off with it. Mobile-app accounts without an organization are not affected.
     */
    private void enforceOrganizationAccess(ClientType clientType, UsersEntity user, boolean isPlatformOwner) {
        if (isPlatformOwner) {
            return;
        }
        if (user.getOrganizationId() == null) {
            if (clientType == ClientType.ADMIN_PANEL) {
                throw new AccessDeniedException(LoginError.ORGANIZATION_REQUIRED.name());
            }
            return;
        }
        OrganizationsEntity organization = organizationRepository.findActiveById(user.getOrganizationId())
                .orElseThrow(() -> new AccessDeniedException(LoginError.ORGANIZATION_NOT_FOUND.name()));
        if (organization.isSuspended()) {
            throw new AccessDeniedException(LoginError.ORGANIZATION_SUSPENDED.name());
        }
    }

    @Override
    public UpdateProfileResponse updateProfile(UpdateProfileRequest request, String email) {
        UsersEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        userMapper.updateEntity(user, request);
        UsersEntity saved = userRepository.save(user);
        return userMapper.toUpdateProfileResponse(saved);
    }

    /**
     * A wrong current password is reported as a field error (400), never as BadCredentialsException (401):
     * clients treat any 401 as an expired session and sign the user out, which a typo must not do.
     */
    @Override
    public void changePassword(ChangePasswordRequest request, String email) {
        UsersEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw fieldError(PasswordChangeError.INCORRECT_CURRENT_PASSWORD);
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw fieldError(PasswordChangeError.NEW_PASSWORD_SAME_AS_CURRENT);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        passwordResetTokenRepository.deleteExpiredBefore(LocalDateTime.now());

        String email = request.getEmail().trim().toLowerCase();
        UsersEntity user = userRepository.findByEmail(email)
                .filter(found -> found.getDeletedAt() == null)
                .orElse(null);
        if (user == null) {
            return;
        }

        passwordResetTokenRepository.deleteByUserId(user.getId());
        String token = generatePasswordResetToken();
        PasswordResetTokenEntity resetToken = new PasswordResetTokenEntity();
        resetToken.setUserId(user.getId());
        resetToken.setTokenHash(hashPasswordResetToken(token));
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(passwordResetExpirationMinutes));
        passwordResetTokenRepository.save(resetToken);
        try {
            emailService.sendPasswordReset(user.getEmail(), user.getFirstName(), token);
        } catch (EmailDeliveryException exception) {
            LOGGER.error("Password reset email delivery failed for user id {} ({})",
                    user.getId(), exception.getClass().getSimpleName());
        }
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String tokenHash = hashPasswordResetToken(request.getToken());
        PasswordResetTokenEntity resetToken = passwordResetTokenRepository
                .findByTokenHashAndUnusedForUpdate(tokenHash)
                .filter(saved -> saved.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> invalidPasswordResetToken());

        UsersEntity user = userRepository.findActiveById(resetToken.getUserId())
                .orElseThrow(() -> invalidPasswordResetToken());
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);
        passwordResetTokenRepository.deleteByUserIdAndIdNot(user.getId(), resetToken.getId());
    }

    private FieldValidationException invalidPasswordResetToken() {
        return new FieldValidationException("token", PasswordResetError.INVALID_OR_EXPIRED.name());
    }

    private String generatePasswordResetToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashPasswordResetToken(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private FieldValidationException fieldError(PasswordChangeError error) {
        return new FieldValidationException(error.getFieldName(), error.name());
    }
}
