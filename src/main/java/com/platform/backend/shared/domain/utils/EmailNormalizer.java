package com.platform.backend.shared.domain.utils;

import java.util.Locale;

/**
 * The one way an email is written before it is stored or looked up. Login already reads the email this way, so an
 * account saved with capitals could never sign in, and the player of an account is found by this same text.
 */
public final class EmailNormalizer {

    private EmailNormalizer() {}

    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
