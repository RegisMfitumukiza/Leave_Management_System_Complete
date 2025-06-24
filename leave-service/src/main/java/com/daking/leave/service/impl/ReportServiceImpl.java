package com.daking.leave.service.impl;

import com.daking.leave.dto.response.ReportResponse;
import com.daking.leave.exception.ReportGenerationException;
import com.daking.leave.exception.ReportNotFoundException;
import com.daking.leave.exception.ValidationException;
import com.daking.leave.model.Leave;
import com.daking.leave.model.Report;
import com.daking.leave.repository.LeaveRepository;
import com.daking.leave.repository.ReportRepository;
import com.daking.leave.service.interfaces.ReportService;
import com.daking.auth.api.dto.UserResponseDTO;
import com.daking.leave.client.UserInfoClient;
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {
    private final ReportRepository reportRepository;
    private final LeaveRepository leaveRepository;
    private final UserInfoClient userInfoClient;

    @Value("${app.reports.directory:./reports}")
    private String reportsDir;

    @Override
    public List<ReportResponse> getReportsByDateRange(LocalDateTime start, LocalDateTime end) {
        log.debug("Fetching reports for date range: {} to {}", start, end);

        validateDateRange(start, end);

        try {
            List<ReportResponse> reports = reportRepository
                    .findByStartDateGreaterThanEqualAndEndDateLessThanEqual(start, end)
                    .stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());

            log.debug("Found {} reports for date range", reports.size());
            return reports;
        } catch (Exception e) {
            log.error("Failed to fetch reports for date range {} to {}: {}", start, end, e.getMessage(), e);
            throw new ReportGenerationException("Failed to fetch reports", e);
        }
    }

    @Override
    public ReportResponse generateEmployeeReport(Long userId, LocalDateTime start, LocalDateTime end, String fileType,
            String generatedBy) {
        log.info("Generating employee report for user {}: {} to {}, type: {}", userId, start, end, fileType);

        validateReportParameters(userId, start, end, fileType, generatedBy);

        String name = "Employee Report - User " + userId;
        String type = "employee";
        String fileName = generateFileName("employee_report_" + userId, fileType);
        String filePath = reportsDir + File.separator + fileName;

        try {
            ensureReportsDirectory();

            List<Leave> leaves = leaveRepository
                    .findByUserIdAndStartDateGreaterThanEqualAndEndDateLessThanEqualWithType(
                            userId, start.toLocalDate(), end.toLocalDate());

            generateReportFile(filePath, fileType, leaves, "employee");

            Report report = createAndSaveReport(name, type, start, end, generatedBy, fileType, filePath, null);
            log.info("Employee report generated successfully with ID: {}", report.getId());
            return toResponse(report);
        } catch (Exception e) {
            log.error("Failed to generate employee report for user {}: {}", userId, e.getMessage(), e);
            throw new ReportGenerationException("Failed to generate employee report", e);
        }
    }

    @Override
    public ReportResponse generateDepartmentReport(Long departmentId, LocalDateTime start, LocalDateTime end,
            String fileType, String generatedBy) {
        log.info("Generating department report for department {}: {} to {}, type: {}", departmentId, start, end,
                fileType);

        validateReportParameters(departmentId, start, end, fileType, generatedBy);

        String name = "Department Report - Dept " + departmentId;
        String type = "department";
        String fileName = generateFileName("department_report_" + departmentId, fileType);
        String filePath = reportsDir + File.separator + fileName;

        try {
            ensureReportsDirectory();

            // Fetch user IDs for department via Feign client
            List<UserResponseDTO> users = userInfoClient.getTeamMembers(departmentId);
            List<Long> userIds = users.stream().map(UserResponseDTO::getId).toList();

            List<Leave> leaves = userIds.stream()
                    .flatMap(uid -> leaveRepository
                            .findByUserIdAndStartDateGreaterThanEqualAndEndDateLessThanEqualWithType(
                                    uid, start.toLocalDate(), end.toLocalDate())
                            .stream())
                    .collect(Collectors.toList());

            generateReportFile(filePath, fileType, leaves, "department");

            Report report = createAndSaveReport(name, type, start, end, generatedBy, fileType, filePath, null);
            log.info("Department report generated successfully with ID: {}", report.getId());
            return toResponse(report);
        } catch (Exception e) {
            log.error("Failed to generate department report for department {}: {}", departmentId, e.getMessage(), e);
            throw new ReportGenerationException("Failed to generate department report", e);
        }
    }

    @Override
    public ReportResponse generateLeaveTypeReport(Long leaveTypeId, LocalDateTime start, LocalDateTime end,
            String fileType, String generatedBy) {
        log.info("Generating leave type report for type {}: {} to {}, type: {}", leaveTypeId, start, end, fileType);

        validateReportParameters(leaveTypeId, start, end, fileType, generatedBy);

        String name = "Leave Type Report - Type " + leaveTypeId;
        String type = "leaveType";
        String fileName = generateFileName("leavetype_report_" + leaveTypeId, fileType);
        String filePath = reportsDir + File.separator + fileName;

        try {
            ensureReportsDirectory();

            List<Leave> leaves = leaveRepository.findByLeaveTypeIdAndStartDateGreaterThanEqualAndEndDateLessThanEqual(
                    leaveTypeId, start.toLocalDate(), end.toLocalDate());

            generateReportFile(filePath, fileType, leaves, "leaveType");

            Report report = createAndSaveReport(name, type, start, end, generatedBy, fileType, filePath, null);
            log.info("Leave type report generated successfully with ID: {}", report.getId());
            return toResponse(report);
        } catch (Exception e) {
            log.error("Failed to generate leave type report for type {}: {}", leaveTypeId, e.getMessage(), e);
            throw new ReportGenerationException("Failed to generate leave type report", e);
        }
    }

    @Override
    public ReportResponse generateTeamLeaveReport(Long managerId, LocalDateTime start, LocalDateTime end,
            String fileType, String generatedBy) {
        log.info("Generating team leave report for manager {}: {} to {}, type: {}", managerId, start, end, fileType);

        validateReportParameters(managerId, start, end, fileType, generatedBy);

        String name = "Team Leave Report - Manager " + managerId;
        String type = "team-leave";
        String fileName = generateFileName("team_leave_report_" + managerId, fileType);
        String filePath = reportsDir + File.separator + fileName;

        try {
            ensureReportsDirectory();

            // Get manager's departments
            List<Long> departmentIds = userInfoClient.getDepartmentsManaged(managerId);
            if (departmentIds == null || departmentIds.isEmpty()) {
                throw new ValidationException("Manager does not manage any departments");
            }

            // Aggregate leaves for all departments
            List<Leave> leaves = departmentIds.stream()
                    .flatMap(deptId -> leaveRepository
                            .findByDepartmentIdAndStartDateGreaterThanEqualAndEndDateLessThanEqualWithType(
                                    deptId, start.toLocalDate(), end.toLocalDate())
                            .stream())
                    .collect(Collectors.toList());

            generateTeamReportFile(filePath, fileType, leaves);

            Report report = createAndSaveReport(name, type, start, end, generatedBy, fileType, filePath, managerId);
            log.info("Team leave report generated successfully with ID: {}", report.getId());
            return toResponse(report);
        } catch (Exception e) {
            log.error("Failed to generate team leave report for manager {}: {}", managerId, e.getMessage(), e);
            throw new ReportGenerationException("Failed to generate team leave report", e);
        }
    }

    @Override
    public ReportResponse generateApprovalStats(Long managerId, LocalDateTime start, LocalDateTime end,
            String fileType, String generatedBy) {
        log.info("Generating approval stats for manager {}: {} to {}, type: {}", managerId, start, end, fileType);

        validateReportParameters(managerId, start, end, fileType, generatedBy);

        String name = "Approval Statistics - Manager " + managerId;
        String type = "approval";
        String fileName = generateFileName("approval_stats_" + managerId, fileType);
        String filePath = reportsDir + File.separator + fileName;

        try {
            ensureReportsDirectory();

            // Get all leaves that need manager's approval in the date range
            List<Leave> leaves = leaveRepository.findByApproverIdAndStartDateGreaterThanEqualAndEndDateLessThanEqual(
                    managerId, start.toLocalDate(), end.toLocalDate());

            generateApprovalStatsFile(filePath, fileType, leaves);

            Report report = createAndSaveReport(name, type, start, end, generatedBy, fileType, filePath, managerId);
            log.info("Approval stats report generated successfully with ID: {}", report.getId());
            return toResponse(report);
        } catch (Exception e) {
            log.error("Failed to generate approval stats for manager {}: {}", managerId, e.getMessage(), e);
            throw new ReportGenerationException("Failed to generate approval stats report", e);
        }
    }

    @Override
    public ReportResponse generateTeamCoverageReport(Long managerId, LocalDateTime start, LocalDateTime end,
            String fileType, String generatedBy) {
        log.info("Generating team coverage report for manager {}: {} to {}, type: {}", managerId, start, end, fileType);

        validateReportParameters(managerId, start, end, fileType, generatedBy);

        String name = "Team Coverage Report - Manager " + managerId;
        String type = "coverage";
        String fileName = generateFileName("team_coverage_" + managerId, fileType);
        String filePath = reportsDir + File.separator + fileName;

        try {
            ensureReportsDirectory();

            // Get manager's departments
            List<Long> departmentIds = userInfoClient.getDepartmentsManaged(managerId);
            if (departmentIds == null || departmentIds.isEmpty()) {
                throw new ValidationException("Manager does not manage any departments");
            }

            // Aggregate team members for all departments
            List<UserResponseDTO> teamMembers = departmentIds.stream()
                    .flatMap(deptId -> userInfoClient.getTeamMembers(deptId).stream())
                    .collect(Collectors.toList());

            // Aggregate leaves for all departments
            List<Leave> leaves = departmentIds.stream()
                    .flatMap(deptId -> leaveRepository
                            .findByDepartmentIdAndStartDateGreaterThanEqualAndEndDateLessThanEqualWithType(
                                    deptId, start.toLocalDate(), end.toLocalDate())
                            .stream())
                    .collect(Collectors.toList());

            generateTeamCoverageFile(filePath, fileType, teamMembers, leaves, start, end);

            Report report = createAndSaveReport(name, type, start, end, generatedBy, fileType, filePath, managerId);
            log.info("Team coverage report generated successfully with ID: {}", report.getId());
            return toResponse(report);
        } catch (Exception e) {
            log.error("Failed to generate team coverage report for manager {}: {}", managerId, e.getMessage(), e);
            throw new ReportGenerationException("Failed to generate team coverage report", e);
        }
    }

    @Override
    public byte[] downloadReport(Long reportId) {
        log.debug("Downloading report with ID: {}", reportId);

        if (reportId == null || reportId <= 0) {
            throw new ValidationException("Report ID must be a positive number");
        }

        try {
            Report report = reportRepository.findById(reportId)
                    .orElseThrow(() -> new ReportNotFoundException("Report not found with ID: " + reportId));

            Path path = Paths.get(report.getFilePath());
            if (!Files.exists(path)) {
                throw new ReportNotFoundException("Report file not found: " + report.getFilePath());
            }

            byte[] content = Files.readAllBytes(path);
            log.info("Report downloaded successfully: {} bytes", content.length);
            return content;
        } catch (ReportNotFoundException e) {
            log.warn("Report not found for download: {}", reportId);
            throw e;
        } catch (Exception e) {
            log.error("Failed to download report {}: {}", reportId, e.getMessage(), e);
            throw new ReportGenerationException("Failed to read report file", e);
        }
    }

    @Override
    public Report getReportById(Long reportId) {
        log.debug("Fetching report by ID: {}", reportId);

        if (reportId == null || reportId <= 0) {
            throw new ValidationException("Report ID must be a positive number");
        }

        try {
            Report report = reportRepository.findById(reportId).orElse(null);
            if (report == null) {
                log.debug("Report not found with ID: {}", reportId);
            }
            return report;
        } catch (Exception e) {
            log.error("Failed to fetch report {}: {}", reportId, e.getMessage(), e);
            throw new ReportGenerationException("Failed to fetch report", e);
        }
    }

    // Utility methods
    private void validateReportParameters(Long id, LocalDateTime start, LocalDateTime end, String fileType,
            String generatedBy) {
        if (id == null || id <= 0) {
            throw new ValidationException("ID must be a positive number");
        }
        validateDateRange(start, end);
        validateFileType(fileType);
        if (!StringUtils.hasText(generatedBy)) {
            throw new ValidationException("Generated by field cannot be empty");
        }
    }

    private void validateDateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null) {
            throw new ValidationException("Start date cannot be null");
        }
        if (end == null) {
            throw new ValidationException("End date cannot be null");
        }
        if (!end.isAfter(start)) {
            throw new ValidationException("End date must be after start date");
        }
    }

    private void validateFileType(String fileType) {
        if (!StringUtils.hasText(fileType)) {
            throw new ValidationException("File type cannot be empty");
        }
        if (!fileType.equalsIgnoreCase("excel") && !fileType.equalsIgnoreCase("csv")) {
            throw new ValidationException("File type must be 'excel' or 'csv'");
        }
    }

    private String generateFileName(String baseName, String fileType) {
        return baseName + "_" + System.currentTimeMillis() +
                (fileType.equalsIgnoreCase("excel") ? ".xlsx" : ".csv");
    }

    private void ensureReportsDirectory() {
        File dir = new File(reportsDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new ReportGenerationException("Failed to create reports directory: " + reportsDir);
        }
    }

    private Report createAndSaveReport(String name, String type, LocalDateTime start, LocalDateTime end,
            String generatedBy, String fileType, String filePath, Long managerId) {
        Report report = new Report();
        report.setName(name);
        report.setType(type);
        report.setStartDate(start);
        report.setEndDate(end);
        report.setGeneratedBy(generatedBy);
        report.setGeneratedAt(LocalDateTime.now());
        report.setFileType(fileType);
        report.setFilePath(filePath);
        report.setManagerId(managerId);
        return reportRepository.save(report);
    }

    private void generateReportFile(String filePath, String fileType, List<Leave> leaves, String reportType)
            throws IOException {
        if (fileType.equalsIgnoreCase("excel")) {
            generateExcelReport(filePath, leaves, reportType);
        } else {
            generateCsvReport(filePath, leaves, reportType);
        }
    }

    private void generateExcelReport(String filePath, List<Leave> leaves, String reportType) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Report");
            Row header = sheet.createRow(0);

            if ("employee".equals(reportType)) {
                header.createCell(0).setCellValue("Leave ID");
                header.createCell(1).setCellValue("Type");
                header.createCell(2).setCellValue("Start Date");
                header.createCell(3).setCellValue("End Date");
                header.createCell(4).setCellValue("Status");

                int rowIdx = 1;
                for (Leave leave : leaves) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(leave.getId());
                    row.createCell(1).setCellValue(leave.getLeaveType().getName());
                    row.createCell(2).setCellValue(leave.getStartDate().toString());
                    row.createCell(3).setCellValue(leave.getEndDate().toString());
                    row.createCell(4).setCellValue(leave.getStatus().name());
                }
            } else {
                header.createCell(0).setCellValue("Leave ID");
                header.createCell(1).setCellValue("User");
                header.createCell(2).setCellValue("Type");
                header.createCell(3).setCellValue("Start Date");
                header.createCell(4).setCellValue("End Date");
                header.createCell(5).setCellValue("Status");

                int rowIdx = 1;
                for (Leave leave : leaves) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(leave.getId());
                    row.createCell(1).setCellValue(leave.getUserId());
                    row.createCell(2).setCellValue(leave.getLeaveType().getName());
                    row.createCell(3).setCellValue(leave.getStartDate().toString());
                    row.createCell(4).setCellValue(leave.getEndDate().toString());
                    row.createCell(5).setCellValue(leave.getStatus().name());
                }
            }

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
        }
    }

    private void generateCsvReport(String filePath, List<Leave> leaves, String reportType) throws IOException {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            if ("employee".equals(reportType)) {
                writer.writeNext(new String[] { "Leave ID", "Type", "Start Date", "End Date", "Status" });
                for (Leave leave : leaves) {
                    writer.writeNext(new String[] {
                            String.valueOf(leave.getId()),
                            leave.getLeaveType().getName(),
                            leave.getStartDate().toString(),
                            leave.getEndDate().toString(),
                            leave.getStatus().name()
                    });
                }
            } else {
                writer.writeNext(new String[] { "Leave ID", "User", "Type", "Start Date", "End Date", "Status" });
                for (Leave leave : leaves) {
                    writer.writeNext(new String[] {
                            String.valueOf(leave.getId()),
                            String.valueOf(leave.getUserId()),
                            leave.getLeaveType().getName(),
                            leave.getStartDate().toString(),
                            leave.getEndDate().toString(),
                            leave.getStatus().name()
                    });
                }
            }
        }
    }

    private void generateTeamReportFile(String filePath, String fileType, List<Leave> leaves) throws IOException {
        if (fileType.equalsIgnoreCase("excel")) {
            generateTeamExcelReport(filePath, leaves);
        } else {
            generateTeamCsvReport(filePath, leaves);
        }
    }

    private void generateTeamExcelReport(String filePath, List<Leave> leaves) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Team Leave Report");

            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Employee ID");
            headerRow.createCell(1).setCellValue("Employee Name");
            headerRow.createCell(2).setCellValue("Leave Type");
            headerRow.createCell(3).setCellValue("Start Date");
            headerRow.createCell(4).setCellValue("End Date");
            headerRow.createCell(5).setCellValue("Status");
            headerRow.createCell(6).setCellValue("Days");

            int rowNum = 1;
            for (Leave leave : leaves) {
                UserResponseDTO employee = userInfoClient.getUserById(leave.getUserId());
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(leave.getUserId());
                row.createCell(1).setCellValue(
                        employee != null ? employee.getFirstName() + " " + employee.getLastName() : "Unknown");
                row.createCell(2).setCellValue(leave.getLeaveType().getName());
                row.createCell(3).setCellValue(leave.getStartDate().toString());
                row.createCell(4).setCellValue(leave.getEndDate().toString());
                row.createCell(5).setCellValue(leave.getStatus().name());
                row.createCell(6).setCellValue(leave.getTotalDays());
            }

            for (int i = 0; i < 7; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
        }
    }

    private void generateTeamCsvReport(String filePath, List<Leave> leaves) throws IOException {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            writer.writeNext(new String[] {
                    "Employee ID", "Employee Name", "Leave Type", "Start Date", "End Date", "Status", "Days"
            });
            for (Leave leave : leaves) {
                UserResponseDTO employee = userInfoClient.getUserById(leave.getUserId());
                writer.writeNext(new String[] {
                        String.valueOf(leave.getUserId()),
                        employee != null ? employee.getFirstName() + " " + employee.getLastName() : "Unknown",
                        leave.getLeaveType().getName(),
                        leave.getStartDate().toString(),
                        leave.getEndDate().toString(),
                        leave.getStatus().name(),
                        String.valueOf(leave.getTotalDays())
                });
            }
        }
    }

    private void generateApprovalStatsFile(String filePath, String fileType, List<Leave> leaves) throws IOException {
        if (fileType.equalsIgnoreCase("excel")) {
            generateApprovalStatsExcel(filePath, leaves);
        } else {
            generateApprovalStatsCsv(filePath, leaves);
        }
    }

    private void generateApprovalStatsExcel(String filePath, List<Leave> leaves) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Summary sheet
            Sheet summarySheet = workbook.createSheet("Summary");
            Row headerRow = summarySheet.createRow(0);
            headerRow.createCell(0).setCellValue("Metric");
            headerRow.createCell(1).setCellValue("Value");

            long totalApplications = leaves.size();
            long approvedCount = leaves.stream().filter(l -> l.getStatus() == Leave.LeaveStatus.APPROVED).count();
            long rejectedCount = leaves.stream().filter(l -> l.getStatus() == Leave.LeaveStatus.REJECTED).count();
            long pendingCount = leaves.stream().filter(l -> l.getStatus() == Leave.LeaveStatus.PENDING).count();
            double approvalRate = totalApplications > 0 ? (double) approvedCount / totalApplications * 100 : 0;

            int rowNum = 1;
            summarySheet.createRow(rowNum++).createCell(0).setCellValue("Total Applications");
            summarySheet.getRow(rowNum - 1).createCell(1).setCellValue(totalApplications);
            summarySheet.createRow(rowNum++).createCell(0).setCellValue("Approved");
            summarySheet.getRow(rowNum - 1).createCell(1).setCellValue(approvedCount);
            summarySheet.createRow(rowNum++).createCell(0).setCellValue("Rejected");
            summarySheet.getRow(rowNum - 1).createCell(1).setCellValue(rejectedCount);
            summarySheet.createRow(rowNum++).createCell(0).setCellValue("Pending");
            summarySheet.getRow(rowNum - 1).createCell(1).setCellValue(pendingCount);
            summarySheet.createRow(rowNum++).createCell(0).setCellValue("Approval Rate (%)");
            summarySheet.getRow(rowNum - 1).createCell(1).setCellValue(approvalRate);

            // Details sheet
            Sheet detailsSheet = workbook.createSheet("Details");
            headerRow = detailsSheet.createRow(0);
            headerRow.createCell(0).setCellValue("Employee");
            headerRow.createCell(1).setCellValue("Leave Type");
            headerRow.createCell(2).setCellValue("Start Date");
            headerRow.createCell(3).setCellValue("End Date");
            headerRow.createCell(4).setCellValue("Status");
            headerRow.createCell(5).setCellValue("Applied On");
            headerRow.createCell(6).setCellValue("Processed On");

            rowNum = 1;
            for (Leave leave : leaves) {
                UserResponseDTO employee = userInfoClient.getUserById(leave.getUserId());
                Row row = detailsSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(
                        employee != null ? employee.getFirstName() + " " + employee.getLastName() : "Unknown");
                row.createCell(1).setCellValue(leave.getLeaveType().getName());
                row.createCell(2).setCellValue(leave.getStartDate().toString());
                row.createCell(3).setCellValue(leave.getEndDate().toString());
                row.createCell(4).setCellValue(leave.getStatus().name());
                row.createCell(5).setCellValue(leave.getCreatedAt().toString());
                row.createCell(6).setCellValue(leave.getUpdatedAt().toString());
            }

            for (int i = 0; i < 7; i++) {
                summarySheet.autoSizeColumn(i);
                detailsSheet.autoSizeColumn(i);
            }

            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
        }
    }

    private void generateApprovalStatsCsv(String filePath, List<Leave> leaves) throws IOException {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            long totalApplications = leaves.size();
            long approvedCount = leaves.stream().filter(l -> l.getStatus() == Leave.LeaveStatus.APPROVED).count();
            long rejectedCount = leaves.stream().filter(l -> l.getStatus() == Leave.LeaveStatus.REJECTED).count();
            long pendingCount = leaves.stream().filter(l -> l.getStatus() == Leave.LeaveStatus.PENDING).count();
            double approvalRate = totalApplications > 0 ? (double) approvedCount / totalApplications * 100 : 0;

            writer.writeNext(new String[] { "Metric", "Value" });
            writer.writeNext(new String[] { "Total Applications", String.valueOf(totalApplications) });
            writer.writeNext(new String[] { "Approved", String.valueOf(approvedCount) });
            writer.writeNext(new String[] { "Rejected", String.valueOf(rejectedCount) });
            writer.writeNext(new String[] { "Pending", String.valueOf(pendingCount) });
            writer.writeNext(new String[] { "Approval Rate (%)", String.format("%.2f", approvalRate) });

            writer.writeNext(new String[] { "" });
            writer.writeNext(new String[] {
                    "Employee", "Leave Type", "Start Date", "End Date", "Status", "Applied On", "Processed On"
            });
            for (Leave leave : leaves) {
                UserResponseDTO employee = userInfoClient.getUserById(leave.getUserId());
                writer.writeNext(new String[] {
                        employee != null ? employee.getFirstName() + " " + employee.getLastName() : "Unknown",
                        leave.getLeaveType().getName(),
                        leave.getStartDate().toString(),
                        leave.getEndDate().toString(),
                        leave.getStatus().name(),
                        leave.getCreatedAt().toString(),
                        leave.getUpdatedAt().toString()
                });
            }
        }
    }

    private void generateTeamCoverageFile(String filePath, String fileType, List<UserResponseDTO> teamMembers,
            List<Leave> leaves, LocalDateTime start, LocalDateTime end) throws IOException {
        if (fileType.equalsIgnoreCase("excel")) {
            generateTeamCoverageExcel(filePath, teamMembers, leaves, start, end);
        } else {
            generateTeamCoverageCsv(filePath, teamMembers, leaves, start, end);
        }
    }

    private void generateTeamCoverageExcel(String filePath, List<UserResponseDTO> teamMembers,
            List<Leave> leaves, LocalDateTime start, LocalDateTime end) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Team Overview sheet
            Sheet overviewSheet = workbook.createSheet("Team Overview");
            Row headerRow = overviewSheet.createRow(0);
            headerRow.createCell(0).setCellValue("Metric");
            headerRow.createCell(1).setCellValue("Value");

            long membersOnLeave = leaves.stream().filter(l -> l.getStatus() == Leave.LeaveStatus.APPROVED).count();
            double coveragePercentage = teamMembers.size() > 0
                    ? (double) (teamMembers.size() - membersOnLeave) / teamMembers.size() * 100
                    : 0;

            int rowNum = 1;
            overviewSheet.createRow(rowNum++).createCell(0).setCellValue("Total Team Members");
            overviewSheet.getRow(rowNum - 1).createCell(1).setCellValue(teamMembers.size());
            overviewSheet.createRow(rowNum++).createCell(0).setCellValue("Members on Leave");
            overviewSheet.getRow(rowNum - 1).createCell(1).setCellValue(membersOnLeave);
            overviewSheet.createRow(rowNum++).createCell(0).setCellValue("Coverage Percentage");
            overviewSheet.getRow(rowNum - 1).createCell(1).setCellValue(coveragePercentage);

            // Team Status sheet
            Sheet statusSheet = workbook.createSheet("Team Status");
            headerRow = statusSheet.createRow(0);
            headerRow.createCell(0).setCellValue("Employee");
            headerRow.createCell(1).setCellValue("Status");
            headerRow.createCell(2).setCellValue("Leave Type");
            headerRow.createCell(3).setCellValue("Start Date");
            headerRow.createCell(4).setCellValue("End Date");
            headerRow.createCell(5).setCellValue("Days");

            rowNum = 1;
            for (UserResponseDTO member : teamMembers) {
                Row row = statusSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(member.getFirstName() + " " + member.getLastName());

                Leave activeLeave = leaves.stream()
                        .filter(l -> l.getUserId().equals(member.getId()) &&
                                l.getStatus() == Leave.LeaveStatus.APPROVED &&
                                !l.getEndDate().isBefore(start.toLocalDate()) &&
                                !l.getStartDate().isAfter(end.toLocalDate()))
                        .findFirst()
                        .orElse(null);

                if (activeLeave != null) {
                    row.createCell(1).setCellValue("On Leave");
                    row.createCell(2).setCellValue(activeLeave.getLeaveType().getName());
                    row.createCell(3).setCellValue(activeLeave.getStartDate().toString());
                    row.createCell(4).setCellValue(activeLeave.getEndDate().toString());
                    row.createCell(5).setCellValue(activeLeave.getTotalDays());
                } else {
                    row.createCell(1).setCellValue("Available");
                    row.createCell(2).setCellValue("");
                    row.createCell(3).setCellValue("");
                    row.createCell(4).setCellValue("");
                    row.createCell(5).setCellValue("");
                }
            }

            for (int i = 0; i < 6; i++) {
                overviewSheet.autoSizeColumn(i);
                statusSheet.autoSizeColumn(i);
            }

            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
        }
    }

    private void generateTeamCoverageCsv(String filePath, List<UserResponseDTO> teamMembers,
            List<Leave> leaves, LocalDateTime start, LocalDateTime end) throws IOException {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            long membersOnLeave = leaves.stream().filter(l -> l.getStatus() == Leave.LeaveStatus.APPROVED).count();
            double coveragePercentage = teamMembers.size() > 0
                    ? (double) (teamMembers.size() - membersOnLeave) / teamMembers.size() * 100
                    : 0;

            writer.writeNext(new String[] { "Metric", "Value" });
            writer.writeNext(new String[] { "Total Team Members", String.valueOf(teamMembers.size()) });
            writer.writeNext(new String[] { "Members on Leave", String.valueOf(membersOnLeave) });
            writer.writeNext(new String[] { "Coverage Percentage", String.format("%.2f", coveragePercentage) });

            writer.writeNext(new String[] { "" });
            writer.writeNext(new String[] {
                    "Employee", "Status", "Leave Type", "Start Date", "End Date", "Days"
            });
            for (UserResponseDTO member : teamMembers) {
                Leave activeLeave = leaves.stream()
                        .filter(l -> l.getUserId().equals(member.getId()) &&
                                l.getStatus() == Leave.LeaveStatus.APPROVED &&
                                !l.getEndDate().isBefore(start.toLocalDate()) &&
                                !l.getStartDate().isAfter(end.toLocalDate()))
                        .findFirst()
                        .orElse(null);

                if (activeLeave != null) {
                    writer.writeNext(new String[] {
                            member.getFirstName() + " " + member.getLastName(),
                            "On Leave",
                            activeLeave.getLeaveType().getName(),
                            activeLeave.getStartDate().toString(),
                            activeLeave.getEndDate().toString(),
                            String.valueOf(activeLeave.getTotalDays())
                    });
                } else {
                    writer.writeNext(new String[] {
                            member.getFirstName() + " " + member.getLastName(),
                            "Available",
                            "",
                            "",
                            "",
                            ""
                    });
                }
            }
        }
    }

    private ReportResponse toResponse(Report report) {
        if (report == null) {
            return null;
        }

        ReportResponse response = new ReportResponse();
        response.setId(report.getId());
        response.setName(report.getName());
        response.setType(report.getType());
        response.setStartDate(report.getStartDate());
        response.setEndDate(report.getEndDate());
        response.setGeneratedBy(report.getGeneratedBy());
        response.setGeneratedAt(report.getGeneratedAt());
        response.setFileType(report.getFileType());
        return response;
    }
}