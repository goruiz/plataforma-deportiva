package com.platform.backend.modules.events.domain.exception;

/**
 * Why a category request was refused. The constant's name is the stable code clients
 * receive as the reason, so they can show it in their own language.
 */
public enum CategoryError {
    NAME_ALREADY_EXISTS,
    IN_USE,

    /** The gender of the rule is not one of {@code Gender} (nor {@code ANY}). */
    INVALID_GENDER,

    /** An age of the rule is above the oldest a person can be ({@code app.people.max-age-years}). */
    AGE_OUT_OF_RANGE,

    /** The minimum age is above the maximum one. */
    AGE_RANGE_INVERTED
}
