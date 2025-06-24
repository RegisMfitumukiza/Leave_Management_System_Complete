package com.daking.leave.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class BulkAdjustRequest {
    private List<Long> userIds;
    private Long leaveTypeId;
    private int adjustmentDays;
    private String reason;
}