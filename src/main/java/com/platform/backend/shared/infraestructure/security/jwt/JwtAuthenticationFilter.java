package com.platform.backend.shared.infraestructure.security.jwt;

import com.platform.backend.modules.users.infrastructure.configuration.security.AuthenticatedUser;
import com.platform.backend.shared.domain.security.SessionChecker;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final SessionChecker sessionChecker;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            authenticateWithToken(authHeader.substring(BEARER_PREFIX.length()), request);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Un token vencido, mal formado o con firma inválida no es un error del servidor: simplemente
     * no autentica la petición. Los endpoints protegidos la rechazan con 401
     * ({@link JwtAuthenticationEntryPoint}) y los públicos —como el login enviado con un token
     * viejo en el header— siguen funcionando.
     */
    private void authenticateWithToken(String jwt, HttpServletRequest request) {
        try {
            final String email = jwtService.extractUsername(jwt);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null
                    && jwtService.isTokenValid(jwt, email)) {
                Claims claims = jwtService.extractAllClaims(jwt);
                if (hasOpenSession(claims)) {
                    setAuthentication(claims, request);
                }
            }
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Ignoring an invalid JWT: {}", ex.getMessage());
        }
    }

    /**
     * A signed token is not enough: its owner may have signed out or deleted the account since it was issued. A token
     * without an account or an issue time cannot be checked, so it does not open a session either.
     */
    private boolean hasOpenSession(Claims claims) {
        Object id = claims.get("id");
        Instant issuedAt = readIssuedAt(claims);
        if (id == null || issuedAt == null) {
            return false;
        }
        return sessionChecker.isSessionValid(UUID.fromString(id.toString()), issuedAt);
    }

    /** The exact moment when the token has it; a token issued before that claim existed only has whole seconds. */
    private Instant readIssuedAt(Claims claims) {
        if (claims.get(JwtClaims.ISSUED_AT_MILLIS) instanceof Number issuedAtMillis) {
            return Instant.ofEpochMilli(issuedAtMillis.longValue());
        }
        return claims.getIssuedAt() == null ? null : claims.getIssuedAt().toInstant();
    }

    private void setAuthentication(Claims claims, HttpServletRequest request) {
        AuthenticatedUser authenticatedUser = buildAuthenticatedUser(claims);
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                authenticatedUser, null, authenticatedUser.getAuthorities()
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    private AuthenticatedUser buildAuthenticatedUser(Claims claims) {
        UUID id = UUID.fromString((String) claims.get("id"));
        String firstName  = (String) claims.get("firstName");
        String middleName = (String) claims.get("middleName");
        String lastName   = (String) claims.get("lastName");
        String secondLastName = (String) claims.get("secondLastName");
        String email = claims.getSubject();

        return AuthenticatedUser.from(
                id, firstName, middleName, lastName, secondLastName, email,
                readOrganizationId(claims),
                isClaimTrue(claims, JwtClaims.ADMIN),
                isClaimTrue(claims, JwtClaims.PLATFORM_OWNER)
        );
    }

    /** A token issued before organizations existed (or to an account without one) has no organization. */
    private UUID readOrganizationId(Claims claims) {
        Object organizationId = claims.get(JwtClaims.ORGANIZATION_ID);
        return organizationId == null ? null : UUID.fromString(organizationId.toString());
    }

    private boolean isClaimTrue(Claims claims, String claimName) {
        return Boolean.TRUE.equals(claims.get(claimName, Boolean.class));
    }
}
