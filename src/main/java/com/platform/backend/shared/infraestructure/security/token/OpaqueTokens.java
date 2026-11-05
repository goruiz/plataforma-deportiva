package com.platform.backend.shared.infraestructure.security.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Single-use secrets that travel by email (password reset, email verification). The raw token goes in the link and only
 * its SHA-256 is stored, so a leaked table cannot be used to open any link.
 */
public final class OpaqueTokens {

    private static final int RANDOM_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private OpaqueTokens() {}

    /** A new URL-safe token with 256 bits of randomness. */
    public static String generate() {
        byte[] randomBytes = new byte[RANDOM_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /** The value that is stored and looked up instead of the token itself. */
    public static String hash(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
