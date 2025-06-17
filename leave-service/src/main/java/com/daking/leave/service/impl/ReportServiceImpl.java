package com.daking.leave.service.impl;

import com.daking.leave.dto.response.ReportResponse;
import com.daking.leave.model.Report;
import com.daking.leave.repository.ReportRepository;
import com.daking.leave.service.interfaces.ReportService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.opencsv.CSVWriter;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.daking.leave.repository.LeaveRepository;
import com.daking.leave.model.Leave;
import com.daking.leave.client.UserInfoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import com.daking.auth.api.dto.UserResponseDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {
    private final ReportRepository reportRepository;
    private final LeaveRepository leaveRepository;
    private final UserInfoClient userInfoClient;

    @Value("${app.reports.directory:./reports}")
    private String reportsDir;

    @Autowired
    public ReportServiceImpl(ReportRepository reportRepository, LeaveRepository leaveRepository,
            UserInfoClient userInfoClient) {
        this.reportRepository = reportRepository;
        this.leaveRepository = leaveRepository;
        this.userInfoClient = userInfoClient;
    }

    @Override
    public List<ReportResponse> getReportsByDateRange(LocalDateTime start, LocalDateTime end) {
        return reportRepository.findByStartDateGreaterThanEqualAndEndDateLessThanEqual(start, end)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public ReportResponse generateEmployeeReport(Long userId, LocalDateTime start, LocalDateTime end, String fileType,
            String generatedBy) {
        String name = "Employee Report - User " + userId;
        String type = "employee";
        String fileName = "employee_report_" + userId + "_" + System.currentTimeMillis()
                + (fileType.equalsIgnoreCase("excel") ? ".xlsx" : ".csv");
        String filePath = reportsDir + File.separator + fileName;
        try {
            // Ensure the reports directory exists
            File dir = new File(reportsDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            List<Leave> leaves = leaveRepository
                    .findByUserIdAndStartDateGreaterThanEqualAndEndDateLessThanEqualWithType(
                            userId, start.toLocalDate(), end.toLocalDate());
            if (fileType.equalsIgnoreCase("excel")) {
                Workbook workbook = new XSSFWorkbook();
                Sheet sheet = workbook.createSheet("Report");
                Row header = sheet.createRow(0);
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
                try (FileOutputStream fos = new FileOutputStream(filePath)) {
                    workbook.write(fos);
                }
                workbook.close();
            } else {
                try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
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
                }
            }
            Report report = new Report();
            report.setName(name);
            report.setType(type);
            report.setStartDate(start);
            report.setEndDate(end);
            report.setGeneratedBy(generatedBy);
            report.setGeneratedAt(LocalDateTime.now());
            report.setFileType(fileType);
            report.setFilePath(filePath);
            report = reportRepository.save(report);
            return toResponse(report);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate report", e);
        }
    }

    @Override
    public ReportResponse generateDepartmentReport(Long departmentId, LocalDateTime start, LocalDateTime end,
            String fileType, String generatedBy) {
        String name = "Department Report - Dept " + departmentId;
        String type = "department";
        String fileName = "department_report_" + departmentId + "_" + System.currentTimeMillis()
                + (fileType.equalsIgnoreCase("excel") ? ".xlsx" : ".csv");
        String filePath = reportsDir + File.separator + fileName;
        try {
            // Ensure the reports directory exists
            File dir = new File(reportsDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            // Fetch user IDs for department via Feign client
            String token = getCurrentToken();
            List<UserResponseDTO> users = userInfoClient.getTeamMembers(departmentId, token);
            List<Long> userIds = users.stream().map(UserResponseDTO::getId).toList();
            List<Leave> leaves = userIds.stream()
                    .flatMap(uid -> leaveRepository
                            .findByUserIdAndStartDateGreaterThanEqualAndEndDateLessThanEqualWithType(
                                    uid, start.toLocalDate(), end.toLocalDate())
                            .stream())
                    .collect(Collectors.toList());
            if (fileType.equalsIgnoreCase("excel")) {
                Workbook workbook = new XSSFWorkbook();
                Sheet sheet = workbook.createSheet("Report");
                Row header = sheet.createRow(0);
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
                try (FileOutputStream fos = new FileOutputStream(filePath)) {
                    workbook.write(fos);
                }
                workbook.close();
            } else {
                try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
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
            Report report = new Report();
            report.setName(name);
            report.setType(type);
            report.setStartDate(start);
            report.setEndDate(end);
            report.setGeneratedBy(generatedBy);
            report.setGeneratedAt(LocalDateTime.now());
            report.setFileType(fileType);
            report.setFilePath(filePath);
            report = reportRepository.save(report);
            return toResponse(report);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate report", e);
        }
    }

    @Override
    public ReportResponse generateLeaveTypeReport(Long leaveTypeId, LocalDateTime start, LocalDateTime end,
            String fileType, String generatedBy) {
        String name = "Leave Type Report - Type " + leaveTypeId;
        String type = "leaveType";
        String fileName = "leavetype_report_" + leaveTypeId + "_" + System.currentTimeMillis()
                + (fileType.equalsIgnoreCase("excel") ? ".xlsx" : ".csv");
        String filePath = reportsDir + File.separator + fileName;
        try {
            // Ensure the reports directory exists
            File dir = new File(reportsDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            List<Leave> leaves = leaveRepository.findByLeaveTypeIdAndStartDateGreaterThanEqualAndEndDateLessThanEqual(
                    leaveTypeId, start.toLocalDate(), end.toLocalDate());
            if (fileType.equalsIgnoreCase("excel")) {
                Workbook workbook = new XSSFWorkbook();
                Sheet sheet = workbook.createSheet("Report");
                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("Leave ID");
                header.createCell(1).setCellValue("User");
                header.createCell(2).setCellValue("Start Date");
                header.createCell(3).setCellValue("End Date");
                header.createCell(4).setCellValue("Status");
                int rowIdx = 1;
                for (Leave leave : leaves) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(leave.getId());
                    row.createCell(1).setCellValue(leave.getUserId());
                    row.createCell(2).setCellValue(leave.getStartDate().toString());
                    row.createCell(3).setCellValue(leave.getEndDate().toString());
                    row.createCell(4).setCellValue(leave.getStatus().name());
                }
                try (FileOutputStream fos = new FileOutputStream(filePath)) {
                    workbook.write(fos);
                }
                workbook.close();
            } else {
                try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
                    writer.writeNext(new String[] { "Leave ID", "User", "Start Date", "End Date", "Status" });
                    for (Leave leave : leaves) {
                        writer.writeNext(new String[] {
                                String.valueOf(leave.getId()),
                                String.valueOf(leave.getUserId()),
                                leave.getStartDate().toString(),
                                leave.getEndDate().toString(),
                                leave.getStatus().name()
                        });
                    }
                }
            }
            Report report = new Report();
            report.setName(name);
            report.setType(type);
            report.setStartDate(start);
            report.setEndDate(end);
            report.setGeneratedBy(generatedBy);
            report.setGeneratedAt(LocalDateTime.now());
            report.setFileType(fileType);
            report.setFilePath(filePath);
            report = reportRepository.save(report);
            return toResponse(report);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate report", e);
        }
    }

    @Override
    public byte[] downloadReport(Long reportId) {
        Report report = reportRepository.findById(reportId).orElseThrow(() -> new RuntimeException("Report not found"));
        try {
            Path path = Paths.get(report.getFilePath());
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read report file", e);
        }
    }

    @Override
    public ReportResponse generateTeamLeaveReport(Long managerId, LocalDateTime start, LocalDateTime end,
            String fileType, String generatedBy) {
        String name = "Team Leave Report - Manager " + managerId;
        String type = "team-leave";
        String fileName = "team_leave_report_" + managerId + "_" + System.currentTimeMillis()
                + (fileType.equalsIgnoreCase("excel") ? ".xlsx" : ".csv");
        String filePath = reportsDir + File.separator + fileName;

        try {
            // Ensure the reports directory exists
            File dir = new File(reportsDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Get manager's departments
            String token = getCurrentToken();
            List<Long> departmentIds = userInfoClient.getDepartmentsManaged(managerId, token);
            if (departmentIds == null || departmentIds.isEmpty()) {
                throw new RuntimeException("Manager does not manage any departments");
            }
            // Aggregate leaves for all departments
            List<Leave> leaves = departmentIds.stream()
                    .flatMap(deptId -> leaveRepository
                            .findByDepartmentIdAndStartDateGreaterThanEqualAndEndDateLessThanEqualWithType(
                                    deptId, start.toLocalDate(), end.toLocalDate())
                            .stream())
                    .collect(java.util.stream.Collectors.toList());

            // Generate report based on file type
            if (fileType.equalsIgnoreCase("excel")) {
                try (Workbook workbook = new XSSFWorkbook()) {
                    Sheet sheet = workbook.createSheet("Team Leave Report");

                    // Create header row
                    Row headerRow = sheet.createRow(0);
                    headerRow.createCell(0).setCellValue("Employee ID");
                    headerRow.createCell(1).setCellValue("Employee Name");
                    headerRow.createCell(2).setCellValue("Leave Type");
                    headerRow.createCell(3).setCellValue("Start Date");
                    headerRow.createCell(4).setCellValue("End Date");
                    headerRow.createCell(5).setCellValue("Status");
                    headerRow.createCell(6).setCellValue("Days");

                    // Add data rows
                    int rowNum = 1;
                    for (Leave leave : leaves) {
                        String leaveUserEmail = userInfoClient.getUserEmail(leave.getUserId(), token);
                        UserResponseDTO employee = userInfoClient.getUserByEmail(leaveUserEmail, token);
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

                    // Auto-size columns
                    for (int i = 0; i < 7; i++) {
                        sheet.autoSizeColumn(i);
                    }

                    // Write to file
                    try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                        workbook.write(fileOut);
                    }
                }
            } else {
                try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
                    writer.writeNext(new String[] {
                            "Employee ID", "Employee Name", "Leave Type", "Start Date", "End Date", "Status", "Days"
                    });
                    for (Leave leave : leaves) {
                        String leaveUserEmail = userInfoClient.getUserEmail(leave.getUserId(), token);
                        UserResponseDTO employee = userInfoClient.getUserByEmail(leaveUserEmail, token);
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
            report = reportRepository.save(report);
            return toResponse(report);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate team leave report", e);
        }
    }

    @Override
    public ReportResponse generateApprovalStats(Long managerId, LocalDateTime start, LocalDateTime end,
            String fileType, String generatedBy) {
        String name = "Approval Statistics - Manager " + managerId;
        String type = "approval";
        String fileName = "approval_stats_" + managerId + "_" + System.currentTimeMillis()
                + (fileType.equalsIgnoreCase("excel") ? ".xlsx" : ".csv");
        String filePath = reportsDir + File.separator + fileName;

        try {
            // Ensure the reports directory exists
            File dir = new File(reportsDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Get all leaves that need manager's approval in the date range
            String token = getCurrentToken();
            List<Leave> leaves = leaveRepository.findByApproverIdAndStartDateGreaterThanEqualAndEndDateLessThanEqual(
                    managerId, start.toLocalDate(), end.toLocalDate());

            // Calculate statistics
            long totalApplications = leaves.size();
            long approvedCount = leaves.stream().filter(l -> l.getStatus() == Leave.LeaveStatus.APPROVED).count();
            long rejectedCount = leaves.stream().filter(l -> l.getStatus() == Leave.LeaveStatus.REJECTED).count();
            long pendingCount = leaves.stream().filter(l -> l.getStatus() == Leave.LeaveStatus.PENDING).count();
            double approvalRate = totalApplications > 0 ? (double) approvedCount / totalApplications * 100 : 0;

            // Generate report based on file type
            if (fileType.equalsIgnoreCase("excel")) {
                try (Workbook workbook = new XSSFWorkbook()) {
                    // Summary sheet
                    Sheet summarySheet = workbook.createSheet("Summary");
                    Row headerRow = summarySheet.createRow(0);
                    headerRow.createCell(0).setCellValue("Metric");
                    headerRow.createCell(1).setCellValue("Value");

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
                        String leaveUserEmail = userInfoClient.getUserEmail(leave.getUserId(), token);
                        UserResponseDTO employee = userInfoClient.getUserByEmail(leaveUserEmail, token);
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

                    // Auto-size columns
                    for (int i = 0; i < 7; i++) {
                        summarySheet.autoSizeColumn(i);
                        detailsSheet.autoSizeColumn(i);
                    }

                    // Write to file
                    try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                        workbook.write(fileOut);
                    }
                }
            } else {
                try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
                    // Write summary
                    writer.writeNext(new String[] { "Metric", "Value" });
                    writer.writeNext(new String[] { "Total Applications", String.valueOf(totalApplications) });
                    writer.writeNext(new String[] { "Approved", String.valueOf(approvedCount) });
                    writer.writeNext(new String[] { "Rejected", String.valueOf(rejectedCount) });
                    writer.writeNext(new String[] { "Pending", String.valueOf(pendingCount) });
                    writer.writeNext(new String[] { "Approval Rate (%)", String.format("%.2f", approvalRate) });

                    // Write details
                    writer.writeNext(new String[] { "" }); // Empty line as separator
                    writer.writeNext(new String[] {
                            "Employee", "Leave Type", "Start Date", "End Date", "Status", "Applied On", "Processed On"
                    });
                    for (Leave leave : leaves) {
                        String leaveUserEmail = userInfoClient.getUserEmail(leave.getUserId(), token);
                        UserResponseDTO employee = userInfoClient.getUserByEmail(leaveUserEmail, token);
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
            report = reportRepository.save(report);
            return toResponse(report);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate approval stats report", e);
        }
    }

    @Override
    public ReportResponse generateTeamCoverageReport(Long managerId, LocalDateTime start, LocalDateTime end,
            String fileType, String generatedBy) {
        String name = "Team Coverage Report - Manager " + managerId;
        String type = "coverage";
        String fileName = "team_coverage_" + managerId + "_" + System.currentTimeMillis()
                + (fileType.equalsIgnoreCase("excel") ? ".xlsx" : ".csv");
        String filePath = reportsDir + File.separator + fileName;

        try {
            // Ensure the reports directory exists
            File dir = new File(reportsDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Get manager's departments
            String token = getCurrentToken();
            List<Long> departmentIds = userInfoClient.getDepartmentsManaged(managerId, token);
            if (departmentIds == null || departmentIds.isEmpty()) {
                throw new RuntimeException("Manager does not manage any departments");
            }
            // Aggregate team members for all departments
            List<UserResponseDTO> teamMembers = departmentIds.stream()
                    .flatMap(deptId -> userInfoClient.getTeamMembers(deptId, token).stream())
                    .collect(java.util.stream.Collectors.toList());
            // Aggregate leaves for all departments
            List<Leave> leaves = departmentIds.stream()
                    .flatMap(deptId -> leaveRepository
                            .findByDepartmentIdAndStartDateGreaterThanEqualAndEndDateLessThanEqualWithType(
                                    deptId, start.toLocalDate(), end.toLocalDate())
                            .stream())
                    .collect(java.util.stream.Collectors.toList());

            // Generate report based on file type
            if (fileType.equalsIgnoreCase("excel")) {
                try (Workbook workbook = new XSSFWorkbook()) {
                    // Team Overview sheet
                    Sheet overviewSheet = workbook.createSheet("Team Overview");
                    Row headerRow = overviewSheet.createRow(0);
                    headerRow.createCell(0).setCellValue("Metric");
                    headerRow.createCell(1).setCellValue("Value");

                    int rowNum = 1;
                    overviewSheet.createRow(rowNum++).createCell(0).setCellValue("Total Team Members");
                    overviewSheet.getRow(rowNum - 1).createCell(1).setCellValue(teamMembers.size());
                    overviewSheet.createRow(rowNum++).createCell(0).setCellValue("Members on Leave");
                    overviewSheet.getRow(rowNum - 1).createCell(1).setCellValue(
                            leaves.stream().filter(l -> l.getStatus() == Leave.LeaveStatus.APPROVED).count());
                    overviewSheet.createRow(rowNum++).createCell(0).setCellValue("Coverage Percentage");
                    overviewSheet.getRow(rowNum - 1).createCell(1).setCellValue(
                            teamMembers.size() > 0
                                    ? (double) (teamMembers.size() - leaves.stream()
                                            .filter(l -> l.getStatus() == Leave.LeaveStatus.APPROVED).count())
                                            / teamMembers.size() * 100
                                    : 0);

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

                        // Find active leave for this member
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

                    // Auto-size columns
                    for (int i = 0; i < 6; i++) {
                        overviewSheet.autoSizeColumn(i);
                        statusSheet.autoSizeColumn(i);
                    }

                    // Write to file
                    try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                        workbook.write(fileOut);
                    }
                }
            } else {
                try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
                    // Write overview
                    writer.writeNext(new String[] { "Metric", "Value" });
                    writer.writeNext(new String[] { "Total Team Members", String.valueOf(teamMembers.size()) });
                    writer.writeNext(new String[] { "Members on Leave",
                            String.valueOf(
                                    leaves.stream().filter(l -> l.getStatus() == Leave.LeaveStatus.APPROVED)
                                            .count()) });
                    writer.writeNext(new String[] { "Coverage Percentage",
                            String.format("%.2f",
                                    teamMembers.size() > 0
                                            ? (double) (teamMembers.size() - leaves.stream()
                                                    .filter(l -> l.getStatus() == Leave.LeaveStatus.APPROVED).count())
                                                    / teamMembers.size() * 100
                                            : 0) });

                    // Write team status
                    writer.writeNext(new String[] { "" }); // Empty line as separator
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
            report = reportRepository.save(report);
            return toResponse(report);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate team coverage report", e);
        }
    }

    @Override
    public Report getReportById(Long reportId) {
        return reportRepository.findById(reportId).orElse(null);
    }

    private ReportResponse toResponse(Report report) {
        ReportResponse resp = new ReportResponse();
        resp.setId(report.getId());
        resp.setName(report.getName());
        resp.setType(report.getType());
        resp.setStartDate(report.getStartDate());
        resp.setEndDate(report.getEndDate());
        resp.setGeneratedBy(report.getGeneratedBy());
        resp.setGeneratedAt(report.getGeneratedAt());
        resp.setFileType(report.getFileType());
        return resp;
    }

    private String getCurrentToken() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null)
            return null;
        Object credentials = auth.getCredentials();
        if (credentials instanceof String token) {
            return "Bearer " + token;
        }
        return null;
    }
}