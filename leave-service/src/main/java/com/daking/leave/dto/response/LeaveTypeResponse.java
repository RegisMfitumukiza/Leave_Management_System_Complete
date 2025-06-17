package com.daking.leave.dto.response;

import lombok.Data;

@Data
public class LeaveTypeResponse {
    private Long id;
    private String name;
    private String description;
    private Double defaultDays;
    private Boolean isActive;
    private Double accrualRate;
    private Boolean canCarryOver;
    private Integer maxCarryOverDays;
    private Boolean requiresApproval;
    private Boolean requiresDocumentation;
    private Boolean isPaid;
}