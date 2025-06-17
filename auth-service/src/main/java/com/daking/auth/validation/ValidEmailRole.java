package com.daking.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidEmailRoleValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEmailRole {
    String message() default "Invalid email role";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}