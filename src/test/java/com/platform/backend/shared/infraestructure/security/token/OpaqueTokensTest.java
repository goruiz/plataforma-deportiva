package com.platform.backend.shared.infraestructure.security.token;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpaqueTokensTest {

    @Test
    void generate_whenCalledTwice_thenTheTokensAreDifferentAndSafeInAUrl() {
        String first = OpaqueTokens.generate();
        String second = OpaqueTokens.generate();

        assertThat(first).isNotEqualTo(second);
        assertThat(first).matches("[A-Za-z0-9_-]{43}");
    }

    @Test
    void hash_whenTheSameTokenIsHashedTwice_thenTheResultIsStableAndNotTheTokenItself() {
        String token = OpaqueTokens.generate();

        assertThat(OpaqueTokens.hash(token)).isEqualTo(OpaqueTokens.hash(token));
        assertThat(OpaqueTokens.hash(token)).isNotEqualTo(token).hasSize(64);
    }

    @Test
    void hash_whenTheTokensDiffer_thenTheHashesDiffer() {
        assertThat(OpaqueTokens.hash("a")).isNotEqualTo(OpaqueTokens.hash("b"));
    }
}
