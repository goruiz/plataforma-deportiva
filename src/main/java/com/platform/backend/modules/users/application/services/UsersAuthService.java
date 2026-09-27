package com.platform.backend.modules.users.application.services;

import com.platform.backend.modules.users.application.iservices.IUsersAuthService;
import com.platform.backend.modules.users.application.mappers.UserMapper;
import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.modules.users.domain.irepositories.IUsersRolesRepository;
import com.platform.backend.modules.users.presentation.requests.LoginRequest.ClientType;
import com.platform.backend.modules.users.presentation.requests.LoginRequest.LoginRequest;
import com.platform.backend.modules.users.presentation.requests.RegisterRequest.RegisterRequest;
import com.platform.backend.modules.users.presentation.requests.UpdateProfileRequest.UpdateProfileRequest;
import com.platform.backend.modules.users.presentation.responses.AuthResponse.AuthResponse;
import com.platform.backend.modules.users.presentation.responses.RegisterResponse.RegisterResponse;
import com.platform.backend.modules.users.presentation.responses.UpdateProfileResponse.UpdateProfileResponse;
import com.platform.backend.shared.infraestructure.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsersAuthService implements IUsersAuthService {

    private final IUserRepository userRepository;
    private final IUsersRolesRepository usersRolesRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${app.security.admin-role-id:}")
    private String adminRoleId;

    @Override
    public RegisterResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new DataIntegrityViolationException("Email already in use");
        }
        UsersEntity newUser = userMapper.toEntity(registerRequest);
        newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        UsersEntity savedUser = userRepository.save(newUser);
        String token = jwtService.generateToken(savedUser, savedUser.getEmail());
        return new RegisterResponse(token);
    }

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        String email = loginRequest.getEmail().trim().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, loginRequest.getPassword())
        );

        UsersEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        boolean isAdmin = isAdmin(user);
        enforceClientTypeAccess(loginRequest.getClientType(), isAdmin);

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId().toString());
        claims.put("firstName", user.getFirstName());
        claims.put("middleName", user.getMiddleName());
        claims.put("lastName", user.getLastName());
        claims.put("secondLastName", user.getSecondLastName());
        claims.put("admin", isAdmin);
        claims.put("clientType", loginRequest.getClientType().name());

        String token = jwtService.generateToken(claims, user.getEmail());
        return new AuthResponse(
                token,
                isAdmin,
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getMiddleName(),
                user.getLastName(),
                user.getSecondLastName()
        );
    }

    /**
     * Admin is determined solely by whether the user has an active users_roles
     * row pointing at the fixed admin role id configured in app.security.admin-role-id —
     * never by role name, since role names are editable, translatable data.
     */
    private boolean isAdmin(UsersEntity user) {
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
            throw new AccessDeniedException("This account does not have administrator access.");
        }
        if (clientType == ClientType.MOBILE_APP && isAdmin) {
            throw new AccessDeniedException("Administrator accounts cannot sign in through the mobile app.");
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
}
