package com.daking.auth.api.dto;

import lombok.Data;
import com.daking.auth.api.model.Role;

@Data
public class UserResponseDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private Long departmentId;
    private Boolean isActive;
    private Boolean isOnLeave;
    private Boolean hasPendingApprovals;
    private Long managerId;
    private String avatarUrl;

    public String getFullName() {
        return (this.firstName != null ? this.firstName : "") + " " + (this.lastName != null ? this.lastName : "");
    }
}