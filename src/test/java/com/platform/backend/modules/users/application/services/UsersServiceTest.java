package com.platform.backend.modules.users.application.services;

import com.platform.backend.modules.users.application.mappers.UserMapper;
import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.modules.users.presentation.requests.LoginRequest.LoginRequest;
import com.platform.backend.modules.users.presentation.requests.RegisterRequest.RegisterRequest;
import com.platform.backend.modules.users.presentation.responses.AuthResponse.AuthResponse;
import com.platform.backend.modules.users.presentation.responses.RegisterResponse.RegisterResponse;
import com.platform.backend.shared.infraestructure.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsersServiceTest {

    @Mock private IUserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;

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

        usersEntity = new UsersEntity();
        usersEntity.setEmail("juan@email.com");
        usersEntity.setUsername("jperez");
        usersEntity.setPassword("hashed_password");
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
        when(jwtService.generateToken(any(), anyString())).thenReturn("token");

        usersAuthService.register(registerRequest);

        verify(passwordEncoder).encode("password123");
    }

    // ─── login ──────────────────────────────────────────────────────────────────

    @Test
    void login_whenValidCredentials_thenReturnToken() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("juan@email.com", "password123"));
        when(userRepository.findByEmail("juan@email.com")).thenReturn(Optional.of(usersEntity));
        when(jwtService.generateToken(usersEntity, "juan@email.com")).thenReturn("jwt-login-token");

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
}
