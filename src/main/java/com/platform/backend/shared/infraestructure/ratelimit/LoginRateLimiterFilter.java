package com.platform.backend.shared.infraestructure.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Slows down, per IP, the requests that can be used to guess a secret (a password, a team code) or to make the server
 * send email. Each path has its own allowance.
 */
public class LoginRateLimiterFilter extends OncePerRequestFilter {

    private record RateLimit(int capacity, Duration window) {}

    private static final Map<String, RateLimit> LIMITS_BY_PATH = Map.of(
            "/users/auth/login", new RateLimit(5, Duration.ofMinutes(1)),
            "/users/auth/forgot-password", new RateLimit(3, Duration.ofMinutes(15)),
            "/users/auth/resend-verification", new RateLimit(3, Duration.ofMinutes(15)),
            "/users/auth/delete-account", new RateLimit(5, Duration.ofMinutes(1)),
            "/players/join-by-code", new RateLimit(10, Duration.ofMinutes(1))
    );

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !LIMITS_BY_PATH.containsKey(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String ip = resolveClientIp(request);
        String path = request.getServletPath();
        Bucket bucket = buckets.computeIfAbsent(ip + ":" + path, k -> buildBucket(LIMITS_BY_PATH.get(path)));

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            writeTooManyRequestsResponse(response);
        }
    }

    private Bucket buildBucket(RateLimit rateLimit) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(rateLimit.capacity())
                .refillIntervally(rateLimit.capacity(), rateLimit.window())
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    // Prioriza X-Forwarded-For para funcionar correctamente detrás de proxies/load balancers
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequestsResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = Map.of(
                "timestamp", System.currentTimeMillis(),
                "status", HttpStatus.TOO_MANY_REQUESTS.value(),
                "error", "Too Many Requests",
                "message", "Too many requests. Please try again later."
        );
        objectMapper.writeValue(response.getWriter(), body);
    }
}
