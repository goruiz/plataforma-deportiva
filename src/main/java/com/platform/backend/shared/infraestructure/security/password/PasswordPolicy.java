package com.platform.backend.shared.infraestructure.security.password;

/**
 * Length rules every user-chosen password must follow. They are constants (not properties)
 * because Bean Validation annotations only accept compile-time values.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;

    // BCrypt only reads the first 72 bytes, so anything longer would be silently truncated (or rejected).
    public static final int MAX_LENGTH = 72;

    public static final int RESET_TOKEN_MAX_LENGTH = 128;

    private PasswordPolicy() {
    }
}
