package com.daking.leave.service.interfaces;

import com.daking.leave.dto.request.LeaveBalanceAdjustmentRequest;
import com.daking.leave.dto.response.LeaveBalanceResponse;
import java.util.List;

public interface LeaveBalanceService {
    List<LeaveBalanceResponse> getLeaveBalancesByUser(Long userId);

    LeaveBalanceResponse adjustLeaveBalance(LeaveBalanceAdjustmentRequest request);

    List<LeaveBalanceResponse> getBulkLeaveBalances(List<Long> userIds);

    LeaveBalanceResponse getLeaveBalance(Long userId, Long leaveTypeId, Integer year);

    void bulkAdjustLeaveBalances(List<Long> userIds, Long leaveTypeId, int adjustmentDays, String reason);

    int initializeMissingLeaveBalances();
}