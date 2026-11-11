package com.platform.backend.shared.domain.enums;

import java.util.Arrays;

/**
 * The gender of a person, and the one a category may ask for. The columns ({@code users.gender}, {@code players.gender},
 * {@code categories.gender}) stay free text, like every state of this project, so {@link #isValid(String)} is how the
 * services check what a client sent.
 */
public enum Gender {
    MALE,
    FEMALE;

    public static boolean isValid(String value) {
        return Arrays.stream(values()).anyMatch(gender -> gender.name().equals(value));
    }
}
