package com.platform.backend.modules.users.presentation.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.platform.backend.modules.users.application.iservices.*;
import com.platform.backend.modules.users.presentation.requests.ChangePasswordRequest.ChangePasswordRequest;
import com.platform.backend.modules.users.presentation.requests.LoginRequest.ClientType;
import com.platform.backend.modules.users.presentation.requests.LoginRequest.LoginRequest;
import com.platform.backend.modules.users.presentation.requests.RegisterRequest.RegisterRequest;
import com.platform.backend.modules.users.presentation.responses.AuthResponse.AuthResponse;
import com.platform.backend.modules.users.presentation.responses.RegisterResponse.RegisterResponse;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import com.platform.backend.shared.infraestructure.exceptions.GlobalException;
import com.platform.backend.shared.infraestructure.security.password.PasswordPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UsersControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private IUsersAuthService usersAuthService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new UsersAuthController(usersAuthService))
                .setControllerAdvice(new GlobalException())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ─── POST /users/auth/register ───────────────────────────────────────────────

    @Test
    void register_whenValidBody_thenReturn200WithToken() throws Exception {
        when(usersAuthService.register(any())).thenReturn(new RegisterResponse("jwt-token"));

        mockMvc.perform(post("/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRegisterRequest("juan@email.com", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("jwt-token"));
    }

    @Test
    void register_whenInvalidEmail_thenReturn400WithFieldError() throws Exception {
        mockMvc.perform(post("/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRegisterRequest("not-an-email", "password123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.email").exists());
    }

    @Test
    void register_whenPasswordTooShort_thenReturn400WithFieldError() throws Exception {
        mockMvc.perform(post("/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRegisterRequest("juan@email.com", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.password").exists());
    }

    @Test
    void register_whenMissingRequiredFields_thenReturn400() throws Exception {
        mockMvc.perform(post("/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").isMap());
    }

    // ─── POST /users/auth/login ──────────────────────────────────────────────────

    @Test
    void login_whenValidCredentials_thenReturn200WithTokenAndAdminFlag() throws Exception {
        when(usersAuthService.login(any())).thenReturn(buildAuthResponse("jwt-login-token", true));

        mockMvc.perform(post("/users/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildLoginRequest("juan@email.com", "password123", ClientType.ADMIN_PANEL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("jwt-login-token"))
                .andExpect(jsonPath("$.data.admin").value(true));
    }

    @Test
    void login_whenBadCredentials_thenReturn401() throws Exception {
        when(usersAuthService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/users/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildLoginRequest("juan@email.com", "wrongpassword", ClientType.ADMIN_PANEL))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void login_whenMissingEmail_thenReturn400() throws Exception {
        LoginRequest request = buildLoginRequest(null, "password123", ClientType.ADMIN_PANEL);

        mockMvc.perform(post("/users/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.email").exists());
    }

    @Test
    void login_whenMissingClientType_thenReturn400() throws Exception {
        LoginRequest request = buildLoginRequest("juan@email.com", "password123", null);

        mockMvc.perform(post("/users/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.clientType").exists());
    }

    // ─── PUT /users/auth/change-password ─────────────────────────────────────────

    @Test
    void changePassword_whenValidBody_thenReturn200AndChangeThePasswordOfTheAuthenticatedUser() throws Exception {
        authenticateAs("juan@email.com");

        mockMvc.perform(put("/users/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildChangePasswordRequest("password123", "brand-new-pass"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(usersAuthService).changePassword(any(), eq("juan@email.com"));
    }

    @Test
    void changePassword_whenCurrentPasswordIsWrong_thenReturn400WithFieldErrorNotAn401() throws Exception {
        authenticateAs("juan@email.com");
        doThrow(new FieldValidationException("currentPassword", "INCORRECT_CURRENT_PASSWORD"))
                .when(usersAuthService).changePassword(any(), eq("juan@email.com"));

        mockMvc.perform(put("/users/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildChangePasswordRequest("wrong", "brand-new-pass"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.currentPassword").value("INCORRECT_CURRENT_PASSWORD"));
    }

    @Test
    void changePassword_whenNewPasswordTooShort_thenReturn400WithFieldError() throws Exception {
        authenticateAs("juan@email.com");

        mockMvc.perform(put("/users/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildChangePasswordRequest("password123", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.newPassword").exists());

        verifyNoInteractions(usersAuthService);
    }

    @Test
    void changePassword_whenNewPasswordTooLongForBcrypt_thenReturn400WithFieldError() throws Exception {
        authenticateAs("juan@email.com");
        String tooLong = "a".repeat(PasswordPolicy.MAX_LENGTH + 1);

        mockMvc.perform(put("/users/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildChangePasswordRequest("password123", tooLong))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.newPassword").exists());
    }

    @Test
    void changePassword_whenMissingFields_thenReturn400WithAFieldErrorForEach() throws Exception {
        authenticateAs("juan@email.com");

        mockMvc.perform(put("/users/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.currentPassword").exists())
                .andExpect(jsonPath("$.data.newPassword").exists());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    /** Stands in for JwtAuthenticationFilter, which is not part of a standalone MockMvc. */
    private void authenticateAs(String email) {
        UserDetails principal = User.withUsername(email).password("unused").authorities("ROLE_USER").build();
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private ChangePasswordRequest buildChangePasswordRequest(String currentPassword, String newPassword) {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword(currentPassword);
        req.setNewPassword(newPassword);
        return req;
    }

    private RegisterRequest buildRegisterRequest(String email, String password) {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }

    private LoginRequest buildLoginRequest(String email, String password, ClientType clientType) {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        req.setClientType(clientType);
        return req;
    }

    private AuthResponse buildAuthResponse(String token, boolean admin) {
        return new AuthResponse(token, admin, UUID.randomUUID(), "juan@email.com", "Juan", null, "Perez", null);
    }
}
