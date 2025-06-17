package com.daking.leave.controller;

import com.daking.leave.dto.response.ReportResponse;
import com.daking.leave.service.interfaces.ReportService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private static final Logger log = LoggerFactory.getLogger(ReportController.class);
    private final ReportService reportService;

    @GetMapping("/date-range")
    public ResponseEntity<List<ReportResponse>> getReportsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        return ResponseEntity.ok(reportService.getReportsByDateRange(start, end));
    }

    @PostMapping("/employee/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ReportResponse> generateEmployeeReport(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String fileType,
            @AuthenticationPrincipal UserDetails userDetails) {
        String generatedBy = (userDetails == null) ? "system" : userDetails.getUsername();
        try {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(23, 59, 59);
            return ResponseEntity.ok(reportService.generateEmployeeReport(userId, start, end, fileType, generatedBy));
        } catch (Exception ex) {
            log.error("Error generating employee report", ex);
            return ResponseEntity.status(500).body(new ReportResponse("Error generating report: " + ex.getMessage()));
        }
    }

    @PostMapping("/department/{departmentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ReportResponse> generateDepartmentReport(
            @PathVariable Long departmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String fileType,
            @AuthenticationPrincipal UserDetails userDetails) {
        String generatedBy = (userDetails == null) ? "system" : userDetails.getUsername();
        try {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(23, 59, 59);
            return ResponseEntity
                    .ok(reportService.generateDepartmentReport(departmentId, start, end, fileType, generatedBy));
        } catch (Exception ex) {
            log.error("Error generating department report", ex);
            return ResponseEntity.status(500).body(new ReportResponse("Error generating report: " + ex.getMessage()));
        }
    }

    @PostMapping("/leave-type/{leaveTypeId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ReportResponse> generateLeaveTypeReport(
            @PathVariable Long leaveTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String fileType,
            @AuthenticationPrincipal UserDetails userDetails) {
        String generatedBy = (userDetails == null) ? "system" : userDetails.getUsername();
        try {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(23, 59, 59);
            return ResponseEntity
                    .ok(reportService.generateLeaveTypeReport(leaveTypeId, start, end, fileType, generatedBy));
        } catch (Exception ex) {
            log.error("Error generating leave type report", ex);
            return ResponseEntity.status(500).body(new ReportResponse("Error generating report: " + ex.getMessage()));
        }
    }

    @PostMapping("/manager/team-leave")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ReportResponse> generateTeamLeaveReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String fileType,
            @AuthenticationPrincipal String userId) {
        if (userId == null) {
            return ResponseEntity.status(401).body(new ReportResponse("User not authenticated"));
        }
        String generatedBy = userId;
        try {
            Long managerId = Long.parseLong(userId);
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(23, 59, 59);
            return ResponseEntity
                    .ok(reportService.generateTeamLeaveReport(managerId, start, end, fileType, generatedBy));
        } catch (Exception ex) {
            log.error("Error generating team leave report", ex);
            return ResponseEntity.status(500).body(new ReportResponse("Error generating report: " + ex.getMessage()));
        }
    }

    @PostMapping("/manager/approval-stats")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ReportResponse> generateApprovalStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String fileType,
            @AuthenticationPrincipal String userId) {
        if (userId == null) {
            return ResponseEntity.status(401).body(new ReportResponse("User not authenticated"));
        }
        String generatedBy = userId;
        try {
            Long managerId = Long.parseLong(userId);
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(23, 59, 59);
            return ResponseEntity.ok(reportService.generateApprovalStats(managerId, start, end, fileType, generatedBy));
        } catch (Exception ex) {
            log.error("Error generating approval stats report", ex);
            return ResponseEntity.status(500).body(new ReportResponse("Error generating report: " + ex.getMessage()));
        }
    }

    @PostMapping("/manager/team-coverage")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ReportResponse> generateTeamCoverageReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String fileType,
            @AuthenticationPrincipal String userId) {
        if (userId == null) {
            return ResponseEntity.status(401).body(new ReportResponse("User not authenticated"));
        }
        String generatedBy = userId;
        try {
            Long managerId = Long.parseLong(userId);
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(23, 59, 59);
            return ResponseEntity
                    .ok(reportService.generateTeamCoverageReport(managerId, start, end, fileType, generatedBy));
        } catch (Exception ex) {
            log.error("Error generating team coverage report", ex);
            return ResponseEntity.status(500).body(new ReportResponse("Error generating report: " + ex.getMessage()));
        }
    }

    @GetMapping("/{reportId}/download")
    public ResponseEntity<byte[]> downloadReport(@PathVariable Long reportId) {
        byte[] file = reportService.downloadReport(reportId);
        var reportOpt = reportService.getReportById(reportId);
        String fileType = reportOpt != null ? reportOpt.getFileType() : "xlsx";
        String extension = "xlsx";
        if (fileType != null && fileType.equalsIgnoreCase("csv")) {
            extension = "csv";
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report_" + reportId + "." + extension)
                .contentType(extension.equals("csv") ? MediaType.TEXT_PLAIN : MediaType.APPLICATION_OCTET_STREAM)
                .body(file);
    }
}