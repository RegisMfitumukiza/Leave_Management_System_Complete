package com.daking.leave.dto.request;

import java.util.List;

public class BulkAdjustRequest {
    private List<Long> userIds;
    private Long leaveTypeId;
    private int adjustmentDays;
    private String reason;

    // Getters and setters
    public List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds;
    }

    public Long getLeaveTypeId() {
        return leaveTypeId;
    }

    public void setLeaveTypeId(Long leaveTypeId) {
        this.leaveTypeId = leaveTypeId;
    }

    public int getAdjustmentDays() {
        return adjustmentDays;
    }

    public void setAdjustmentDays(int adjustmentDays) {
        this.adjustmentDays = adjustmentDays;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}