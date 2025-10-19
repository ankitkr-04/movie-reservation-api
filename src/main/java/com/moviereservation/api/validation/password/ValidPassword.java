package com.moviereservation.api.validation.password;

import java.lang.annotation.*;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordValidator.class)
@Documented
public @interface ValidPassword {
    String message() default "Password must be at least 8 characters long and include at least one uppercase letter, one lowercase letter, one digit, and one special character.";

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

}
