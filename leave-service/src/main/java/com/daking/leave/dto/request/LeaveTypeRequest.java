package com.daking.leave.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class LeaveTypeRequest {
    @NotBlank
    private String name;
    private String description;
    @NotNull
    private Double defaultDays;
    private Boolean isActive = true;
    private Double accrualRate = 0.0;
    private Boolean canCarryOver = false;
    private Integer maxCarryOverDays = 0;
    private Boolean requiresApproval = true;
    private Boolean requiresDocumentation = false;
    private Boolean isPaid = true;
}