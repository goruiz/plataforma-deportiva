package com.platform.backend.shared.infraestructure.security.password;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TemporaryPasswordGenerator {

    private static final int MINIMUM_LENGTH = 8;

    // Visually ambiguous characters (0/O, 1/l/I) are left out so the password can be read from an email without mistakes.
    private static final String UPPERCASE_LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWERCASE_LETTERS = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String ALL_CHARACTERS = UPPERCASE_LETTERS + LOWERCASE_LETTERS + DIGITS;

    private final SecureRandom secureRandom = new SecureRandom();
    private final int length;

    public TemporaryPasswordGenerator(@Value("${app.security.temporary-password-length:12}") int length) {
        if (length < MINIMUM_LENGTH) {
            throw new IllegalArgumentException(
                    "app.security.temporary-password-length must be at least " + MINIMUM_LENGTH);
        }
        this.length = length;
    }

    /** Always contains at least one uppercase letter, one lowercase letter and one digit. */
    public String generate() {
        List<Character> characters = new ArrayList<>(length);
        characters.add(pickRandomCharacterFrom(UPPERCASE_LETTERS));
        characters.add(pickRandomCharacterFrom(LOWERCASE_LETTERS));
        characters.add(pickRandomCharacterFrom(DIGITS));
        while (characters.size() < length) {
            characters.add(pickRandomCharacterFrom(ALL_CHARACTERS));
        }

        // Shuffle so the guaranteed characters don't always sit at the start.
        Collections.shuffle(characters, secureRandom);
        return characters.stream().map(String::valueOf).collect(Collectors.joining());
    }

    private char pickRandomCharacterFrom(String allowedCharacters) {
        return allowedCharacters.charAt(secureRandom.nextInt(allowedCharacters.length()));
    }
}
