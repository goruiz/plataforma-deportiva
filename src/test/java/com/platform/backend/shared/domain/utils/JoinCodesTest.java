package com.platform.backend.shared.domain.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JoinCodesTest {

    @Test
    void normalize_whenTypedWithHyphenSpacesAndLowerCase_thenItIsTheSameCode() {
        assertThat(JoinCodes.normalize(" ab3d-9fk2 ")).isEqualTo("AB3D9FK2");
        assertThat(JoinCodes.normalize("AB3D 9FK2")).isEqualTo("AB3D9FK2");
    }

    @Test
    void normalize_whenThereIsNothingTyped_thenItIsEmpty() {
        assertThat(JoinCodes.normalize(null)).isEmpty();
        assertThat(JoinCodes.normalize(" - ")).isEmpty();
    }

    @Test
    void alphabet_whenRead_thenItLeavesOutTheCharactersThatAreMistakenForEachOther() {
        assertThat(JoinCodes.ALPHABET).doesNotContain("0", "O", "1", "I");
    }
}
