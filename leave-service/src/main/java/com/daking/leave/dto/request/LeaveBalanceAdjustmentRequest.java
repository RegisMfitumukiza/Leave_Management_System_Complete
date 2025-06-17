package com.daking.leave.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class LeaveBalanceAdjustmentRequest {
    @NotNull
    private Long userId;
    @NotNull
    private Long leaveTypeId;
    @NotNull
    private Double adjustmentDays;
    private String reason;
}