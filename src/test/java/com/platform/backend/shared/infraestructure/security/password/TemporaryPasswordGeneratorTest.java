package com.platform.backend.shared.infraestructure.security.password;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemporaryPasswordGeneratorTest {

    private static final int CONFIGURED_LENGTH = 12;

    private final TemporaryPasswordGenerator generator = new TemporaryPasswordGenerator(CONFIGURED_LENGTH);

    @Test
    void generate_thenReturnPasswordWithConfiguredLength() {
        assertThat(generator.generate()).hasSize(CONFIGURED_LENGTH);
    }

    @Test
    void generate_thenIncludeUppercaseLowercaseAndDigit() {
        for (int attempt = 0; attempt < 200; attempt++) {
            String password = generator.generate();

            assertThat(password).matches(".*[A-Z].*").matches(".*[a-z].*").matches(".*[0-9].*");
        }
    }

    @Test
    void generate_thenNeverUseAmbiguousCharacters() {
        for (int attempt = 0; attempt < 200; attempt++) {
            assertThat(generator.generate()).doesNotContainPattern("[0O1lI]");
        }
    }

    @Test
    void generate_calledTwice_thenReturnDifferentPasswords() {
        assertThat(generator.generate()).isNotEqualTo(generator.generate());
    }

    @Test
    void constructor_whenLengthIsBelowMinimum_thenFailFast() {
        assertThatThrownBy(() -> new TemporaryPasswordGenerator(7))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
