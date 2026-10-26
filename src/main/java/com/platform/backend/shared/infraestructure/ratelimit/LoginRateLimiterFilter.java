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

public class LoginRateLimiterFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/users/auth/login";
    private static final String PASSWORD_RESET_REQUEST_PATH = "/users/auth/forgot-password";
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final int MAX_PASSWORD_RESET_REQUESTS = 3;
    private static final Duration PASSWORD_RESET_WINDOW = Duration.ofMinutes(15);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !isRateLimitedPath(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String ip = resolveClientIp(request);
        String path = request.getServletPath();
        Bucket bucket = buckets.computeIfAbsent(ip + ":" + path, k -> buildBucket(path));

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            writeTooManyRequestsResponse(response);
        }
    }

    private boolean isRateLimitedPath(String path) {
        return LOGIN_PATH.equals(path) || PASSWORD_RESET_REQUEST_PATH.equals(path);
    }

    private Bucket buildBucket(String path) {
        int capacity = PASSWORD_RESET_REQUEST_PATH.equals(path)
                ? MAX_PASSWORD_RESET_REQUESTS
                : MAX_ATTEMPTS;
        Duration window = PASSWORD_RESET_REQUEST_PATH.equals(path)
                ? PASSWORD_RESET_WINDOW
                : WINDOW;
        Bandwidth limit = Bandwidth.builder().capacity(capacity).refillIntervally(capacity, window).build();
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
