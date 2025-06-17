package com.daking.auth.validation;

import com.daking.auth.model.User;
import org.springframework.stereotype.Component;

@Component
public class EmailRoleValidator {

    public boolean validateEmailRole(User user) {
        if (user.getEmail() == null || user.getRole() == null) {
            return false;
        }

        String localPart = user.getEmail().substring(0, user.getEmail().indexOf("@"));
        return switch (user.getRole()) {
            case STAFF -> localPart.endsWith(".staff");
            case MANAGER -> localPart.endsWith(".manager");
            case ADMIN -> localPart.endsWith(".admin");
            default -> false;
        };
    }
}