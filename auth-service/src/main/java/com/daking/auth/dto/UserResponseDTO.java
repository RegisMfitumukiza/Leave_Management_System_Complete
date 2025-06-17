package com.daking.auth.dto;

import lombok.Data;
import com.daking.auth.model.Role;

@Data
public class UserResponseDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String username;
    private Role role;
    private Long departmentId;
    private Boolean isActive;
    private Boolean isOnLeave;
    private Boolean hasPendingApprovals;
    private Long managerId;
    private String avatarUrl;
    private DepartmentDTO department;
}