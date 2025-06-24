package com.daking.auth.validation;

import com.daking.auth.model.User;
import org.springframework.stereotype.Component;

@Component
public class EmailRoleValidator {

    public boolean validateEmailRole(User user) {
        if (user.getEmail() == null || user.getRole() == null) {
            return false;
        }

        String email = user.getEmail();
        int atIndex = email.indexOf("@");
        if (atIndex == -1 || atIndex == 0) {
            return false; // Invalid email format
        }

        String localPart = email.substring(0, atIndex);
        return switch (user.getRole()) {
            case STAFF -> localPart.endsWith(".staff");
            case MANAGER -> localPart.endsWith(".manager");
            case ADMIN -> localPart.endsWith(".admin");
            default -> false;
        };
    }
}