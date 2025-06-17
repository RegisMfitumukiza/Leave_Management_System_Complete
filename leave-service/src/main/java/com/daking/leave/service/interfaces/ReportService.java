package com.daking.leave.service.interfaces;

import com.daking.leave.dto.response.ReportResponse;
import com.daking.leave.model.Report;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportService {
        List<ReportResponse> getReportsByDateRange(LocalDateTime start, LocalDateTime end);

        ReportResponse generateEmployeeReport(Long userId, LocalDateTime start, LocalDateTime end, String fileType,
                        String generatedBy);

        ReportResponse generateDepartmentReport(Long departmentId, LocalDateTime start, LocalDateTime end,
                        String fileType,
                        String generatedBy);

        ReportResponse generateLeaveTypeReport(Long leaveTypeId, LocalDateTime start, LocalDateTime end,
                        String fileType,
                        String generatedBy);

        // Manager-specific report methods
        ReportResponse generateTeamLeaveReport(Long managerId, LocalDateTime start, LocalDateTime end,
                        String fileType, String generatedBy);

        ReportResponse generateApprovalStats(Long managerId, LocalDateTime start, LocalDateTime end,
                        String fileType, String generatedBy);

        ReportResponse generateTeamCoverageReport(Long managerId, LocalDateTime start, LocalDateTime end,
                        String fileType, String generatedBy);

        byte[] downloadReport(Long reportId);

        Report getReportById(Long reportId);
}