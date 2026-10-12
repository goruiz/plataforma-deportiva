package com.platform.backend.shared.infraestructure.security.jwt;

import com.platform.backend.modules.users.infrastructure.configuration.security.SecurityConfig;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Levanta la cadena de seguridad real ({@link SecurityConfig} + filtro JWT + entry point) con un
 * controller de mentira, para comprobar qué ve el cliente: un 401 con {@code ApiResponse} cuando la
 * sesión no es válida, y endpoints públicos que siguen funcionando aunque llegue un token viejo.
 */
@SpringJUnitWebConfig(classes = {
        SecurityConfig.class,
        JwtService.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class,
        JwtSecurityChainTest.StubWebConfig.class
})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-long-enough-for-hmac-sha-256-signing",
        "jwt.expiration=60000"
})
class JwtSecurityChainTest {

    private static final String PROTECTED_PATH = "/protected";
    private static final String PUBLIC_LOGIN_PATH = "/users/auth/login";

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Filter securityFilterChain = webApplicationContext.getBean("springSecurityFilterChain", Filter.class);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(securityFilterChain)
                .build();
    }

    @Test
    void protectedEndpoint_whenThereIsNoToken_thenRespondUnauthorizedWithApiResponse() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void protectedEndpoint_whenTokenIsExpired_thenRespondUnauthorized() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Bearer " + issueToken(-1_000)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void protectedEndpoint_whenTokenIsMalformed_thenRespondUnauthorized() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_whenTokenIsValid_thenRespondOk() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Bearer " + issueToken(60_000)))
                .andExpect(status().isOk());
    }

    @Test
    void publicEndpoint_whenAnExpiredTokenIsSent_thenStillRespondOk() throws Exception {
        mockMvc.perform(post(PUBLIC_LOGIN_PATH).header("Authorization", "Bearer " + issueToken(-1_000)))
                .andExpect(status().isOk());
    }

    private String issueToken(long expirationMillis) {
        JwtService issuer = new JwtService();
        ReflectionTestUtils.setField(issuer, "secret", ReflectionTestUtils.getField(jwtService, "secret"));
        ReflectionTestUtils.setField(issuer, "jwtExpiration", expirationMillis);
        return issuer.generateToken(Map.of("id", UUID.randomUUID().toString()), "juan@email.com");
    }

    @Configuration
    @EnableWebMvc
    static class StubWebConfig {

        @Bean
        UserDetailsService userDetailsService() {
            return new InMemoryUserDetailsManager();
        }

        @Bean
        StubController stubController() {
            return new StubController();
        }
    }

    @RestController
    static class StubController {

        @GetMapping(PROTECTED_PATH)
        String protectedEndpoint() {
            return "ok";
        }

        @PostMapping(PUBLIC_LOGIN_PATH)
        String login() {
            return "ok";
        }
    }
}
