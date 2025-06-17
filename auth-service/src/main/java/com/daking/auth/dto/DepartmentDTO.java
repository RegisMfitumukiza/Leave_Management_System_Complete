package com.daking.auth.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DepartmentDTO {
    private Long id;
    private String name;
    private String description;
    private boolean active;
    private Long parentDepartmentId;
    private Long managerId;
    private String managerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<UserSummaryDTO> users;
}