package com.platform.backend.shared.infraestructure.security.jwt;

import com.platform.backend.modules.users.infrastructure.configuration.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-key-long-enough-for-hmac-sha-256-signing";
    private static final String OTHER_SECRET = "another-secret-key-long-enough-for-hmac-sha-256";
    private static final String USER_EMAIL = "juan@email.com";
    private static final long VALID_FOR_MILLIS = 60_000;
    private static final long ALREADY_EXPIRED_MILLIS = -1_000;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(buildJwtService(SECRET, VALID_FOR_MILLIS));
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_whenTokenIsValid_thenAuthenticateTheRequestAndContinue() throws Exception {
        request.addHeader("Authorization", bearer(buildJwtService(SECRET, VALID_FOR_MILLIS)));

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(AuthenticatedUser.class);
        assertThat(((AuthenticatedUser) authentication.getPrincipal()).getEmail()).isEqualTo(USER_EMAIL);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void doFilter_whenTokenIsExpired_thenDoNotAuthenticateAndContinue() throws Exception {
        request.addHeader("Authorization", bearer(buildJwtService(SECRET, ALREADY_EXPIRED_MILLIS)));

        filter.doFilter(request, response, chain);

        assertContinuesWithoutAuthentication();
    }

    @Test
    void doFilter_whenTokenIsMalformed_thenDoNotAuthenticateAndContinue() throws Exception {
        request.addHeader("Authorization", "Bearer this-is-not-a-jwt");

        filter.doFilter(request, response, chain);

        assertContinuesWithoutAuthentication();
    }

    @Test
    void doFilter_whenTokenIsSignedWithAnotherKey_thenDoNotAuthenticateAndContinue() throws Exception {
        request.addHeader("Authorization", bearer(buildJwtService(OTHER_SECRET, VALID_FOR_MILLIS)));

        filter.doFilter(request, response, chain);

        assertContinuesWithoutAuthentication();
    }

    @Test
    void doFilter_whenThereIsNoAuthorizationHeader_thenDoNotAuthenticateAndContinue() throws Exception {
        filter.doFilter(request, response, chain);

        assertContinuesWithoutAuthentication();
    }

    @Test
    void doFilter_whenTokenIsOfAnOrganizationAdministrator_thenThePrincipalBelongsToThatOrganizationWithAdminAuthority() throws Exception {
        UUID organizationId = UUID.randomUUID();
        request.addHeader("Authorization", bearerWith(Map.of(
                "admin", true, "platformOwner", false, "organizationId", organizationId.toString())));

        filter.doFilter(request, response, chain);

        AuthenticatedUser principal = authenticatedUser();
        assertThat(principal.getOrganizationId()).isEqualTo(organizationId);
        assertThat(principal.isPlatformOwner()).isFalse();
        assertThat(principal.getAuthorities()).extracting("authority").containsExactly("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void doFilter_whenTokenIsOfThePlatformOwner_thenThePrincipalHasTheOwnerAuthority() throws Exception {
        request.addHeader("Authorization", bearerWith(Map.of("admin", true, "platformOwner", true)));

        filter.doFilter(request, response, chain);

        AuthenticatedUser principal = authenticatedUser();
        assertThat(principal.getOrganizationId()).isNull();
        assertThat(principal.isPlatformOwner()).isTrue();
        assertThat(principal.getAuthorities()).extracting("authority")
                .containsExactly("ROLE_USER", "ROLE_ADMIN", "ROLE_PLATFORM_OWNER");
    }

    @Test
    void doFilter_whenTokenHasNoOrganizationClaims_thenThePrincipalIsAPlainUser() throws Exception {
        request.addHeader("Authorization", bearer(buildJwtService(SECRET, VALID_FOR_MILLIS)));

        filter.doFilter(request, response, chain);

        AuthenticatedUser principal = authenticatedUser();
        assertThat(principal.getOrganizationId()).isNull();
        assertThat(principal.isPlatformOwner()).isFalse();
        assertThat(principal.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    private AuthenticatedUser authenticatedUser() {
        return (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private String bearerWith(Map<String, Object> extraClaims) {
        Map<String, Object> claims = new java.util.HashMap<>(extraClaims);
        claims.put("id", UUID.randomUUID().toString());
        return "Bearer " + buildJwtService(SECRET, VALID_FOR_MILLIS).generateToken(claims, USER_EMAIL);
    }

    private void assertContinuesWithoutAuthentication() {
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private JwtService buildJwtService(String secret, long expirationMillis) {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", secret);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", expirationMillis);
        return jwtService;
    }

    private String bearer(JwtService tokenIssuer) {
        Map<String, Object> claims = Map.of("id", UUID.randomUUID().toString());
        return "Bearer " + tokenIssuer.generateToken(claims, USER_EMAIL);
    }
}
