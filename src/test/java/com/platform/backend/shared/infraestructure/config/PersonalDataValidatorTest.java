package com.platform.backend.shared.infraestructure.config;

import com.platform.backend.shared.domain.exception.PersonalDataError;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersonalDataValidatorTest {

    private final PersonalDataValidator validator = new PersonalDataValidator(new PersonAgeLimits(120));

    @Test
    void nothingSaidIsNothingWrong() {
        assertThatCode(() -> validator.requireValid(null, null)).doesNotThrowAnyException();
    }

    @Test
    void aRealGenderAndBirthDateAreAccepted() {
        assertThatCode(() -> validator.requireValid("FEMALE", LocalDate.now().minusYears(30))).doesNotThrowAnyException();
        assertThatCode(() -> validator.requireValid("MALE", LocalDate.now())).as("born today").doesNotThrowAnyException();
    }

    @Test
    void aGenderThatIsNotOneWeKnowIsRefusedOnItsField() {
        assertThatThrownBy(() -> validator.requireValid("female", null))
                .isInstanceOf(FieldValidationException.class)
                .satisfies(error -> assertThat(((FieldValidationException) error).getFieldErrors())
                        .containsEntry("gender", PersonalDataError.INVALID_GENDER.name()));
    }

    @Test
    void aBirthDateInTheFutureOrOfSomeoneTooOldIsRefusedOnItsField() {
        assertThatThrownBy(() -> validator.requireValid(null, LocalDate.now().plusDays(1)))
                .satisfies(error -> assertThat(((FieldValidationException) error).getFieldErrors())
                        .containsEntry("birthDate", PersonalDataError.INVALID_BIRTH_DATE.name()));
        assertThatThrownBy(() -> validator.requireValid(null, LocalDate.now().minusYears(121)))
                .isInstanceOf(FieldValidationException.class);
    }
}
