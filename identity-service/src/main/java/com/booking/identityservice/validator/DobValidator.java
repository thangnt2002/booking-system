package com.booking.identityservice.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class DobValidator implements ConstraintValidator<DobConstraint, LocalDate> {
    int min;
    @Override
    public void initialize(DobConstraint constraintAnnotation) {
        min = constraintAnnotation.min();
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(LocalDate localDate, ConstraintValidatorContext constraintValidatorContext) {
        if (Objects.isNull(localDate)) {
            return true;
        }
        return ChronoUnit.YEARS.between(localDate, LocalDate.now()) >= min;
    }
}
