package com.platform.backend.modules.users.domain.exception;

/**
 * Why a password change was refused. The constant's name is the stable code clients receive
 * as the reason, so they can show it in their own language; the field says where to show it.
 */
public enum PasswordChangeError {
    INCORRECT_CURRENT_PASSWORD("currentPassword"),
    NEW_PASSWORD_SAME_AS_CURRENT("newPassword");

    private final String fieldName;

    PasswordChangeError(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
