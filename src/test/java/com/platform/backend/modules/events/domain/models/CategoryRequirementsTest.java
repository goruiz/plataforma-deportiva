package com.platform.backend.modules.events.domain.models;

import com.platform.backend.modules.events.domain.exception.CategoryEligibilityError;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryRequirementsTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);

    private static Optional<CategoryEligibilityError> check(CategoryRequirements requirements, String gender, LocalDate birthDate) {
        return requirements.check(gender, birthDate, TODAY);
    }

    @Test
    void aCategoryWithoutRulesAdmitsEveryoneEvenWithoutData() {
        assertThat(CategoryRequirements.NONE.isEmpty()).isTrue();
        assertThat(check(CategoryRequirements.NONE, null, null)).isEmpty();
    }

    @Test
    void aGenderRuleAdmitsOnlyThatGender() {
        CategoryRequirements womenOnly = new CategoryRequirements("FEMALE", null, null);

        assertThat(check(womenOnly, "FEMALE", null)).isEmpty();
        assertThat(check(womenOnly, "MALE", null)).contains(CategoryEligibilityError.CATEGORY_GENDER_NOT_ALLOWED);
    }

    @Test
    void aGenderRuleNeedsToKnowTheGenderOfThePerson() {
        assertThat(check(new CategoryRequirements("MALE", null, null), null, null))
                .contains(CategoryEligibilityError.CATEGORY_GENDER_REQUIRED);
    }

    @Test
    void anAgeRuleNeedsToKnowTheBirthDate() {
        assertThat(check(new CategoryRequirements(null, 12, null), null, null))
                .contains(CategoryEligibilityError.CATEGORY_BIRTH_DATE_REQUIRED);
        assertThat(check(new CategoryRequirements(null, null, 15), null, null))
                .contains(CategoryEligibilityError.CATEGORY_BIRTH_DATE_REQUIRED);
    }

    @Test
    void theAgeRangeIncludesBothLimits() {
        CategoryRequirements from12To15 = new CategoryRequirements(null, 12, 15);

        assertThat(check(from12To15, null, TODAY.minusYears(12))).as("turned 12 today").isEmpty();
        assertThat(check(from12To15, null, TODAY.minusYears(15))).as("turned 15 today").isEmpty();
        assertThat(check(from12To15, null, TODAY.minusYears(11))).contains(CategoryEligibilityError.CATEGORY_TOO_YOUNG);
        assertThat(check(from12To15, null, TODAY.minusYears(16))).contains(CategoryEligibilityError.CATEGORY_TOO_OLD);
    }

    @Test
    void theAgeIsTheYearsCompletedNotTheYearOfBirth() {
        CategoryRequirements atLeast12 = new CategoryRequirements(null, 12, null);

        // Born on 11 November 2014: the day before the 12th birthday, still 11.
        assertThat(check(atLeast12, null, LocalDate.of(2014, 11, 11))).contains(CategoryEligibilityError.CATEGORY_TOO_YOUNG);
        assertThat(check(atLeast12, null, LocalDate.of(2014, 11, 10))).isEmpty();
    }

    @Test
    void aSingleLimitLeavesTheOtherOpen() {
        assertThat(check(new CategoryRequirements(null, 18, null), null, TODAY.minusYears(70))).isEmpty();
        assertThat(check(new CategoryRequirements(null, null, 10), null, TODAY.minusYears(1))).isEmpty();
    }

    @Test
    void theGenderIsJudgedBeforeTheAge() {
        CategoryRequirements womenFrom12 = new CategoryRequirements("FEMALE", 12, null);

        assertThat(check(womenFrom12, "MALE", TODAY.minusYears(5))).contains(CategoryEligibilityError.CATEGORY_GENDER_NOT_ALLOWED);
        assertThat(check(womenFrom12, "FEMALE", TODAY.minusYears(5))).contains(CategoryEligibilityError.CATEGORY_TOO_YOUNG);
    }
}
