package com.platform.backend.shared.infraestructure.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // ====================== MÉTODOS PRINCIPALES (GENÉRICOS) ======================

    /**
     * Genera un token JWT a partir de CUALQUIER entidad
     * Convierte automáticamente los campos del objeto en claims
     */
    public String generateToken(Object entity, String subject) {
        Map<String, Object> claims = objectMapper.convertValue(entity, Map.class);

        // Remover campos sensibles automáticamente
        removeSensitiveFields(claims);

        return createToken(claims, subject);
    }

    /**
     * Método más flexible: permite pasar los claims manualmente
     */
    public String generateToken(Map<String, Object> claims, String subject) {
        return createToken(claims, subject);
    }

    // ====================== Token Creation ======================
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    // ====================== Validación y Extracción ======================
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // ====================== Utilidad ======================
    private void removeSensitiveFields(Map<String, Object> claims) {
        claims.remove("password");
        claims.remove("Password");
        claims.remove("secret");
        claims.remove("token");
        claims.remove("refreshToken");
        claims.remove("accessToken");
    }
}