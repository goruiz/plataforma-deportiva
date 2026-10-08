package com.platform.backend.modules.users.application.services;

import com.platform.backend.modules.users.application.iservices.IUsersManagementService;
import com.platform.backend.modules.users.application.mappers.UserMapper;
import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.modules.users.presentation.requests.CreateUserRequest.CreateUserRequest;
import com.platform.backend.modules.users.presentation.requests.UpdateUserRequest.UpdateUserRequest;
import com.platform.backend.modules.users.presentation.responses.UserResponse.UserResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import com.platform.backend.shared.domain.utils.SoftDeleteHelper;
import com.platform.backend.shared.infraestructure.email.EmailService;
import com.platform.backend.shared.infraestructure.security.password.TemporaryPasswordGenerator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsersManagementService implements IUsersManagementService {

    private final IUserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final EmailService emailService;

    @Value("${app.security.admin-role-id:}")
    private String adminRoleId;

    @Override
    public List<UserResponse> getAllUsers() {
        return toUserResponses(userRepository.findAllActive());
    }

    /**
     * Same admin rule as login: an active users_roles row pointing at the id in
     * app.security.admin-role-id — never the role's name. With no admin role
     * configured nobody is an admin (mirrors UsersAuthService#isAdmin).
     */
    @Override
    public List<UserResponse> getAdminUsers() {
        if (!isAdminRoleConfigured()) {
            return List.of();
        }
        return toUserResponses(userRepository.findAllActiveWithRole(UUID.fromString(adminRoleId)));
    }

    /**
     * Everyone who is not an admin — including users with no role at all,
     * which is the normal state of an account created from the mobile app.
     */
    @Override
    public List<UserResponse> getRegularUsers() {
        if (!isAdminRoleConfigured()) {
            return getAllUsers();
        }
        return toUserResponses(userRepository.findAllActiveWithoutRole(UUID.fromString(adminRoleId)));
    }

    @Override
    public UserResponse getUserById(UUID id) {
        UsersEntity user = userRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        return userMapper.toUserResponse(user);
    }

    /**
     * The admin never chooses the password: a temporary one is generated and emailed to the new user.
     * Transactional on purpose — if the email can't be sent the user is rolled back, so no account
     * is left behind whose password nobody knows.
     */
    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DataIntegrityViolationException("Email already in use");
        }
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new DataIntegrityViolationException("Username already in use");
        }

        String temporaryPassword = temporaryPasswordGenerator.generate();
        UsersEntity user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        UsersEntity savedUser = userRepository.save(user);

        emailService.sendUserCreatedWithTemporaryPassword(
                savedUser.getEmail(), savedUser.getFirstName(), temporaryPassword);
        return userMapper.toUserResponse(savedUser);
    }

    @Override
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        UsersEntity user = userRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new DataIntegrityViolationException("Email already in use");
        }
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())
                && userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new DataIntegrityViolationException("Username already in use");
        }

        userMapper.updateEntity(user, request);
        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Override
    public void softDelete(UUID id) {
        UsersEntity user = userRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public void hardDelete(UUID id) {
        UsersEntity user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        userRepository.hardDelete(user);
    }

    private boolean isAdminRoleConfigured() {
        return StringUtils.hasText(adminRoleId);
    }

    private List<UserResponse> toUserResponses(List<UsersEntity> users) {
        return users.stream()
                .map(userMapper::toUserResponse)
                .toList();
    }
}

