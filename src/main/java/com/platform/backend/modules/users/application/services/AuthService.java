package com.platform.backend.modules.users.application.services;

import com.platform.backend.modules.users.application.iservices.IAuthService;
import com.platform.backend.modules.users.application.mappers.UserMapper;
import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.presentation.requests.RegisterRequest.RegisterRequest;
import com.platform.backend.modules.users.presentation.responses.RegisterResponse.RegisterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.platform.backend.modules.users.infrastructure.persistence.jpa.UsersJpaRepository;
import com.platform.backend.shared.infraestructure.security.jwt.JwtService;

@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    private final UsersJpaRepository usersJpaRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public RegisterResponse register(RegisterRequest registerRequest) {

        UsersEntity newRegisteredUser = userMapper.toEntity(registerRequest);

        newRegisteredUser.setPassword(
                passwordEncoder.encode(registerRequest.getPassword())
        );

        UsersEntity savedUser = usersJpaRepository.save(newRegisteredUser);

        String token = jwtService.generateToken(
                savedUser,
                savedUser.getUsername()
        );

        return new RegisterResponse(token);
    }
}