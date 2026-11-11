package com.platform.backend.shared.infraestructure.config;

import com.platform.backend.shared.domain.enums.Gender;
import com.platform.backend.shared.domain.exception.PersonalDataError;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * The one place that decides whether the gender and birth date a client sent can be real, for every way a person gives
 * them (registering, completing the profile, registering with a team invitation).
 */
@Component
@RequiredArgsConstructor
public class PersonalDataValidator {

    private static final String GENDER_FIELD = "gender";
    private static final String BIRTH_DATE_FIELD = "birthDate";

    private final PersonAgeLimits ageLimits;

    /** A null says nothing (an update leaves what was stored), so only a value that was sent is judged. */
    public void requireValid(String gender, LocalDate birthDate) {
        if (gender != null && !Gender.isValid(gender)) {
            throw new FieldValidationException(GENDER_FIELD, PersonalDataError.INVALID_GENDER.name());
        }
        if (birthDate != null && !ageLimits.isPossibleBirthDate(birthDate, LocalDate.now())) {
            throw new FieldValidationException(BIRTH_DATE_FIELD, PersonalDataError.INVALID_BIRTH_DATE.name());
        }
    }
}
