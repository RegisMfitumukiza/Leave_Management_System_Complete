package com.daking.auth.dto;

import lombok.Data;
import com.daking.auth.model.User;

@Data
public class UserSummaryDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;

    public static UserSummaryDTO fromUser(User user) {
        UserSummaryDTO dto = new UserSummaryDTO();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        return dto;
    }
}