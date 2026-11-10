package com.platform.backend.modules.audit.domain.models;

import java.util.regex.Pattern;

/**
 * Deleting a record frees its unique name by appending {@code _DELETED_<id>} to it (see {@code HasUniqueField}). That
 * tag is plumbing, not part of what the record was called, so the audit trail shows the name without it.
 */
public final class DeletedNameTag {

    private static final Pattern TAG = Pattern.compile("_DELETED_[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$");

    private DeletedNameTag() {
    }

    /** The text without the tag a deletion added at its end; null and text without a tag come back as they are. */
    public static String strip(String text) {
        return text == null ? null : TAG.matcher(text).replaceFirst("");
    }
}
