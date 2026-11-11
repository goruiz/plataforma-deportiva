package com.platform.backend.modules.events.domain.exception;

/**
 * Why a person may not play in a category. The constant's name is the stable code clients receive (as the {@code message}
 * of a 400, or as the reason of a roster line), so they can explain it in their own language.
 */
public enum CategoryEligibilityError {
    /** The category is for the other gender. */
    CATEGORY_GENDER_NOT_ALLOWED,

    /** The category asks for a gender and the person has not said theirs yet. */
    CATEGORY_GENDER_REQUIRED,

    /** The category asks for an age and the person has not said when they were born yet. */
    CATEGORY_BIRTH_DATE_REQUIRED,

    /** The person is younger than the category's minimum age. */
    CATEGORY_TOO_YOUNG,

    /** The person is older than the category's maximum age. */
    CATEGORY_TOO_OLD
}
