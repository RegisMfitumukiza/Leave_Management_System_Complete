package com.daking.leave.service.interfaces;

import com.daking.leave.dto.request.LeaveApplicationRequest;
import com.daking.leave.dto.request.LeaveApprovalRequest;
import com.daking.leave.dto.response.LeaveResponse;
import java.util.List;

public interface LeaveService {
    LeaveResponse applyForLeave(String userEmail, LeaveApplicationRequest request);

    LeaveResponse approveLeave(Long leaveId, String approverEmail, LeaveApprovalRequest request);

    LeaveResponse rejectLeave(Long leaveId, String approverEmail, LeaveApprovalRequest request);

    LeaveResponse cancelLeave(Long leaveId, String userEmail);

    LeaveResponse getLeaveById(Long leaveId);

    List<LeaveResponse> getLeavesByUser(String userEmail);

    List<LeaveResponse> getPendingLeaves(String managerEmail);

    List<LeaveResponse> getTeamCalendar(Long departmentId, String month);

    List<LeaveResponse> getTeamCalendarForManager(String managerEmail, String month);

    List<LeaveResponse> getStaffTeamCalendar(Long userId, String month);

    List<LeaveResponse> searchLeaves(String query);

    List<LeaveResponse> getRecentLeaves();

    int countAllLeaves();

    int countLeavesByStatus(String status);

    List<LeaveResponse> getLeavesByUserId(Long userId);

    List<LeaveResponse> getAllLeaves();

    List<LeaveResponse> getLeavesByUserIds(List<Long> userIds);
}