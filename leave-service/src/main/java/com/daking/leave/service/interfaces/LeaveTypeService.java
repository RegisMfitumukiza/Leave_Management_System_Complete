package com.daking.leave.service.interfaces;

import com.daking.leave.dto.request.LeaveTypeRequest;
import com.daking.leave.dto.response.LeaveTypeResponse;
import java.util.List;

public interface LeaveTypeService {
    LeaveTypeResponse createLeaveType(LeaveTypeRequest request);

    LeaveTypeResponse updateLeaveType(Long id, LeaveTypeRequest request);

    LeaveTypeResponse getLeaveTypeById(Long id);

    List<LeaveTypeResponse> getAllLeaveTypes();

    List<LeaveTypeResponse> getActiveLeaveTypes();

    void deleteLeaveType(Long id);
}