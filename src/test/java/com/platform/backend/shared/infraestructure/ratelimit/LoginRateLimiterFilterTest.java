package com.platform.backend.shared.infraestructure.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** Each path that can be used to guess a secret or to make the server send email has its own allowance per IP. */
class LoginRateLimiterFilterTest {

    private LoginRateLimiterFilter filter;

    @BeforeEach
    void setUp() {
        filter = new LoginRateLimiterFilter();
    }

    private int post(String path, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setServletPath(path);
        request.addHeader("X-Forwarded-For", ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }

    @Test
    void resendVerification_whenAskedFourTimesInARow_thenTheFourthIsTooManyRequests() throws Exception {
        assertThat(post("/users/auth/resend-verification", "10.0.0.1")).isEqualTo(200);
        assertThat(post("/users/auth/resend-verification", "10.0.0.1")).isEqualTo(200);
        assertThat(post("/users/auth/resend-verification", "10.0.0.1")).isEqualTo(200);

        assertThat(post("/users/auth/resend-verification", "10.0.0.1")).isEqualTo(429);
    }

    @Test
    void joinByCode_whenAskedElevenTimesInAMinute_thenTheEleventhIsTooManyRequests() throws Exception {
        for (int attempt = 0; attempt < 10; attempt++) {
            assertThat(post("/players/join-by-code", "10.0.0.2")).isEqualTo(200);
        }

        assertThat(post("/players/join-by-code", "10.0.0.2")).isEqualTo(429);
    }

    @Test
    void deleteAccount_whenAskedSixTimesInAMinute_thenTheSixthIsTooManyRequests() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            assertThat(post("/users/auth/delete-account", "10.0.0.3")).isEqualTo(200);
        }

        assertThat(post("/users/auth/delete-account", "10.0.0.3")).isEqualTo(429);
    }

    @Test
    void limits_whenTheIpDiffersOrThePathHasNoLimit_thenTheyAreNotShared() throws Exception {
        for (int attempt = 0; attempt < 3; attempt++) {
            post("/users/auth/resend-verification", "10.0.0.4");
        }

        assertThat(post("/users/auth/resend-verification", "10.0.0.5")).isEqualTo(200);
        assertThat(post("/users/auth/login", "10.0.0.4")).isEqualTo(200);
        for (int attempt = 0; attempt < 30; attempt++) {
            assertThat(post("/teams", "10.0.0.4")).isEqualTo(200);
        }
    }

    @Test
    void limits_whenTheRequestIsNotAPost_thenItIsNeverLimited() throws Exception {
        for (int attempt = 0; attempt < 20; attempt++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/players/join-by-code");
            request.setServletPath("/players/join-by-code");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }
}
