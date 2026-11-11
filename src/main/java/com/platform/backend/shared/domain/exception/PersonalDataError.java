package com.platform.backend.shared.domain.exception;

/**
 * Why the personal data a person gave (gender, birth date) was refused. The constant's name is the stable code clients
 * receive as the reason of the offending field ({@code gender} or {@code birthDate}).
 */
public enum PersonalDataError {
    /** The gender is not one of {@code Gender}. */
    INVALID_GENDER,

    /** The birth date is in the future, or makes the person older than anyone can be. */
    INVALID_BIRTH_DATE
}
