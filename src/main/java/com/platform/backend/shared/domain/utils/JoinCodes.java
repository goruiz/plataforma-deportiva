package com.platform.backend.shared.domain.utils;

import java.util.Locale;

/**
 * The code a team gives its players to join without an invitation. It is typed by hand, so the alphabet leaves out the
 * characters that are mistaken for each other (0 and O, 1 and I) and whatever is typed is read leniently.
 */
public final class JoinCodes {

    public static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private JoinCodes() {}

    /** Upper case and without spaces or hyphens, so "ab3d-9fk2" and "AB3D9FK2" are the same code. */
    public static String normalize(String typed) {
        if (typed == null) {
            return "";
        }
        return typed.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }
}
