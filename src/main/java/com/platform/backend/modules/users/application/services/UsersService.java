package com.platform.backend.modules.users.application.services;

import com.platform.backend.modules.users.application.iservices.IUsersService;
import com.platform.backend.modules.users.application.mappers.UserMapper;
import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.modules.users.presentation.requests.LoginRequest.LoginRequest;
import com.platform.backend.modules.users.presentation.requests.RegisterRequest.RegisterRequest;
import com.platform.backend.modules.users.presentation.requests.UpdateProfileRequest.UpdateProfileRequest;
import com.platform.backend.modules.users.presentation.responses.AuthResponse.AuthResponse;
import com.platform.backend.modules.users.presentation.responses.RegisterResponse.RegisterResponse;
import com.platform.backend.modules.users.presentation.responses.UpdateProfileResponse.UpdateProfileResponse;
import com.platform.backend.modules.users.presentation.responses.UserResponse.UserResponse;
import com.platform.backend.shared.infraestructure.security.jwt.JwtService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsersService implements IUsersService {

    private final IUserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAllActive().stream()
                .map(userMapper::toUserResponse)
                .toList();
    }

    @Override
    public UserResponse getUserById(UUID id) {
        UsersEntity user = userRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        return userMapper.toUserResponse(user);
    }

    @Override
    public RegisterResponse register(RegisterRequest registerRequest) {
        UsersEntity newUser = userMapper.toEntity(registerRequest);
        newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        UsersEntity savedUser = userRepository.save(newUser);
        String token = jwtService.generateToken(savedUser, savedUser.getEmail());
        return new RegisterResponse(token);
    }

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        UsersEntity user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        String token = jwtService.generateToken(user, user.getEmail());
        return new AuthResponse(token);
    }

    @Override
    public UpdateProfileResponse updateProfile(UpdateProfileRequest request, String email) {
        UsersEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        userMapper.updateEntity(user, request);
        UsersEntity saved = userRepository.save(user);
        return userMapper.toUpdateProfileResponse(saved);
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
}
