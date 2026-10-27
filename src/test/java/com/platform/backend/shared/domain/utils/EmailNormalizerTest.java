package com.platform.backend.shared.domain.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailNormalizerTest {

    @Test
    void normalize_whenTheEmailHasCapitalsAndSpaces_thenItIsTrimmedAndLowerCased() {
        assertThat(EmailNormalizer.normalize("  Juan.Perez@Email.COM ")).isEqualTo("juan.perez@email.com");
    }

    @Test
    void normalize_whenTheEmailIsAlreadyNormalized_thenItIsNotChanged() {
        assertThat(EmailNormalizer.normalize("juan@email.com")).isEqualTo("juan@email.com");
    }

    @Test
    void normalize_whenThereIsNoEmail_thenThereIsNothingToNormalize() {
        assertThat(EmailNormalizer.normalize(null)).isNull();
    }
}
