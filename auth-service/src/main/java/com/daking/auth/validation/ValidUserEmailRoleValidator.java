package com.daking.auth.validation;

import com.daking.auth.dto.RegistrationDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidUserEmailRoleValidator implements ConstraintValidator<ValidUserEmailRole, RegistrationDto> {
    @Override
    public boolean isValid(RegistrationDto dto, ConstraintValidatorContext context) {
        if (dto == null)
            return true; // Let @NotNull handle nulls
        String email = dto.getEmail();
        String role = dto.getRole();
        if (email == null || role == null)
            return true; // Let other validators handle nulls
        int atIndex = email.indexOf("@");
        if (atIndex == -1 || atIndex == 0)
            return false;
        String localPart = email.substring(0, atIndex);
        switch (role.toUpperCase()) {
            case "STAFF":
                return localPart.endsWith(".staff");
            case "MANAGER":
                return localPart.endsWith(".manager");
            case "ADMIN":
                return localPart.endsWith(".admin");
            default:
                return false;
        }
    }
}