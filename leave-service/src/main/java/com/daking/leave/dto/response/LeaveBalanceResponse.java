package com.daking.leave.dto.response;

import lombok.Data;

@Data
public class LeaveBalanceResponse {
    private Long id;
    private Long userId;
    private Long leaveTypeId;
    private String leaveTypeName;
    private Integer year;
    private Double totalDays;
    private Double usedDays;
    private Double remainingDays;
    private Double carriedOverDays;
}