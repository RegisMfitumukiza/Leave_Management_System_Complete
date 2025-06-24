package com.daking.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidUserEmailRoleValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUserEmailRole {
    String message() default "Email local part must end with .staff, .manager, or .admin according to the selected role.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}