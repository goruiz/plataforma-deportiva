package com.platform.backend.shared.infraestructure.security.jwt;

import com.platform.backend.modules.users.infrastructure.configuration.security.AuthenticatedUser;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        final String email = jwtService.extractUsername(jwt);

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null
                && jwtService.isTokenValid(jwt, email)) {

            AuthenticatedUser authenticatedUser = buildAuthenticatedUser(jwt);
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    authenticatedUser, null, authenticatedUser.getAuthorities()
            );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }

    private AuthenticatedUser buildAuthenticatedUser(String jwt) {
        Claims claims = jwtService.extractAllClaims(jwt);

        UUID id = UUID.fromString((String) claims.get("id"));
        String firstName  = (String) claims.get("firstName");
        String middleName = (String) claims.get("middleName");
        String lastName   = (String) claims.get("lastName");
        String secondLastName = (String) claims.get("secondLastName");
        String email = claims.getSubject();

        return AuthenticatedUser.from(id, firstName, middleName, lastName, secondLastName, email, "ROLE_USER");
    }
}
