package com.platform.backend.shared.infraestructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;

/**
 * How old a person can be ({@code app.people.max-age-years}): what keeps a birth date, and the age a category asks for,
 * from being something that cannot be real.
 */
@Component
public class PersonAgeLimits {

    private final int maxAgeYears;

    public PersonAgeLimits(@Value("${app.people.max-age-years:120}") int maxAgeYears) {
        this.maxAgeYears = maxAgeYears;
    }

    public int maxAgeYears() {
        return maxAgeYears;
    }

    /** An age a category may ask for: from 0 to the oldest a person can be. */
    public boolean isPossibleAge(int age) {
        return age >= 0 && age <= maxAgeYears;
    }

    /** A birth date that is not in the future and does not make the person older than the oldest one possible. */
    public boolean isPossibleBirthDate(LocalDate birthDate, LocalDate today) {
        return !birthDate.isAfter(today) && Period.between(birthDate, today).getYears() <= maxAgeYears;
    }
}
