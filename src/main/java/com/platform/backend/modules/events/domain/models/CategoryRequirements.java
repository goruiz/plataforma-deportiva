package com.platform.backend.modules.events.domain.models;

import com.platform.backend.modules.events.domain.exception.CategoryEligibilityError;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

/**
 * What a category asks of whoever plays in it: a gender and/or an age range, in years completed. A null means "no
 * condition of that kind", so a category without rules admits everyone. Plain data and rules, no persistence: the same
 * check serves a player who joins a team, a match event and the roster of a team.
 *
 * @param gender {@code MALE} or {@code FEMALE}; null admits both
 * @param minAge the youngest age admitted (inclusive); null has no minimum
 * @param maxAge the oldest age admitted (inclusive); null has no maximum
 */
public record CategoryRequirements(String gender, Integer minAge, Integer maxAge) {

    /** A category that asks nothing of anyone. */
    public static final CategoryRequirements NONE = new CategoryRequirements(null, null, null);

    /** What a client sends to say "both genders" (an update ignores a null, so it needs a value to take the rule away). */
    public static final String ANY_GENDER = "ANY";

    public boolean isEmpty() {
        return gender == null && minAge == null && maxAge == null;
    }

    /**
     * The reason the person may not play in the category, or empty when they may. The gender is checked first; a person
     * who has not given the data a rule needs cannot be told to fit it, so that is a reason of its own.
     */
    public Optional<CategoryEligibilityError> check(String personGender, LocalDate birthDate, LocalDate today) {
        Optional<CategoryEligibilityError> genderProblem = checkGender(personGender);
        if (genderProblem.isPresent()) {
            return genderProblem;
        }
        return checkAge(birthDate, today);
    }

    private Optional<CategoryEligibilityError> checkGender(String personGender) {
        if (gender == null) {
            return Optional.empty();
        }
        if (personGender == null) {
            return Optional.of(CategoryEligibilityError.CATEGORY_GENDER_REQUIRED);
        }
        return gender.equals(personGender)
                ? Optional.empty()
                : Optional.of(CategoryEligibilityError.CATEGORY_GENDER_NOT_ALLOWED);
    }

    private Optional<CategoryEligibilityError> checkAge(LocalDate birthDate, LocalDate today) {
        if (minAge == null && maxAge == null) {
            return Optional.empty();
        }
        if (birthDate == null) {
            return Optional.of(CategoryEligibilityError.CATEGORY_BIRTH_DATE_REQUIRED);
        }
        int age = Period.between(birthDate, today).getYears();
        if (minAge != null && age < minAge) {
            return Optional.of(CategoryEligibilityError.CATEGORY_TOO_YOUNG);
        }
        if (maxAge != null && age > maxAge) {
            return Optional.of(CategoryEligibilityError.CATEGORY_TOO_OLD);
        }
        return Optional.empty();
    }
}
