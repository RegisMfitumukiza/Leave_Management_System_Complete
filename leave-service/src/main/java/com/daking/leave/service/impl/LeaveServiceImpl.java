package com.daking.leave.service.impl;

import com.daking.auth.api.dto.UserResponseDTO;
import com.daking.auth.api.model.Role;
import com.daking.leave.client.UserInfoClient;
import com.daking.leave.dto.request.LeaveApplicationRequest;
import com.daking.leave.dto.request.LeaveApprovalRequest;
import com.daking.leave.dto.response.LeaveResponse;
import com.daking.leave.model.*;
import com.daking.leave.repository.LeaveBalanceRepository;
import com.daking.leave.repository.LeaveRepository;
import com.daking.leave.repository.LeaveTypeRepository;
import com.daking.leave.service.interfaces.DocumentService;
import com.daking.leave.service.interfaces.InAppNotificationService;
import com.daking.leave.service.interfaces.LeaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRepository leaveRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final UserInfoClient userInfoClient;
    private final InAppNotificationService inAppNotificationService;
    private final DocumentService documentService;

    @Override
    @Transactional
    public LeaveResponse applyForLeave(String userEmail, LeaveApplicationRequest request) {
        UserResponseDTO user = userInfoClient.getUserByEmail(userEmail);
        if (user == null) {
            throw new IllegalArgumentException("User not found with email: " + userEmail);
        }

        LeaveType leaveType = leaveTypeRepository.findById(request.getLeaveTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Leave type not found"));

        // Get current year for leave balance
        LeaveBalance balance = leaveBalanceRepository
                .findByUserIdAndLeaveTypeAndYear(user.getId(), leaveType, java.time.LocalDate.now().getYear())
                .orElseThrow(() -> new IllegalArgumentException("Leave balance not found for user"));

        if (balance.getRemainingDays() < java.time.temporal.ChronoUnit.DAYS.between(request.getStartDate(),
                request.getEndDate()) + 1) {
            throw new IllegalArgumentException("Insufficient leave balance.");
        }

        Leave leave = new Leave();
        leave.setUserId(user.getId());
        leave.setLeaveType(leaveType);
        leave.setStartDate(request.getStartDate());
        leave.setEndDate(request.getEndDate());
        leave.setTotalDays(
                (double) (java.time.temporal.ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate())
                        + 1));
        leave.setReason(request.getReason());
        leave.setStatus(Leave.LeaveStatus.PENDING);
        leave.setDepartmentId(user.getDepartmentId());

        if (request.getDocumentIds() != null && !request.getDocumentIds().isEmpty()) {
            List<Document> docs = documentService.getDocumentsByIds(request.getDocumentIds());
            leave.setDocuments(docs);
        }

        leave = leaveRepository.save(leave);

        balance.setRemainingDays(balance.getRemainingDays()
                - (double) (java.time.temporal.ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate())
                        + 1));
        leaveBalanceRepository.save(balance);

        try {
            List<UserResponseDTO> managers = userInfoClient.getManagers(user.getDepartmentId());
            if (managers != null && !managers.isEmpty()) {
                String applicantName = user.getFirstName() + " " + user.getLastName();
                String message = String.format("New leave application from %s needs your review.", applicantName);
                for (UserResponseDTO manager : managers) {
                    inAppNotificationService.sendNotification(manager.getId(), message, "LEAVE_APPLICATION",
                            leave.getId(), "/approvals/leave");
                }
            }
        } catch (Exception e) {
            log.error("Could not send notification to manager for new leave application.", e);
        }

        return toLeaveResponse(leave);
    }

    @Override
    @Transactional
    public LeaveResponse approveLeave(Long leaveId, String approverEmail, LeaveApprovalRequest request) {
        UserResponseDTO approver = userInfoClient.getUserByEmail(approverEmail);
        if (approver == null) {
            throw new IllegalArgumentException("Approver not found with email: " + approverEmail);
        }

        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new IllegalArgumentException("Leave application not found"));

        if (leave.getStatus() != Leave.LeaveStatus.PENDING) {
            throw new IllegalStateException("Leave request is not in a pending state.");
        }

        UserResponseDTO applicant = userInfoClient.getUserById(leave.getUserId());
        if (applicant == null) {
            throw new IllegalArgumentException("Applicant not found for leave request.");
        }

        if (!isApproverAuthorized(approver, applicant.getDepartmentId())) {
            throw new SecurityException("Approver is not authorized for this leave request.");
        }

        leave.setStatus(Leave.LeaveStatus.APPROVED);
        leave.setApproverId(approver.getId());
        leave.setComments(request.getComments());
        leave = leaveRepository.save(leave);

        String message = String.format("Your leave request for %s has been approved.", leave.getLeaveType().getName());
        inAppNotificationService.sendNotification(applicant.getId(), message, "LEAVE_STATUS", leave.getId(),
                "/leave/history");

        return toLeaveResponse(leave);
    }

    @Override
    @Transactional
    public LeaveResponse rejectLeave(Long leaveId, String approverEmail, LeaveApprovalRequest request) {
        UserResponseDTO approver = userInfoClient.getUserByEmail(approverEmail);
        if (approver == null) {
            throw new IllegalArgumentException("Approver not found with email: " + approverEmail);
        }

        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new IllegalArgumentException("Leave application not found"));

        if (leave.getStatus() != Leave.LeaveStatus.PENDING) {
            throw new IllegalStateException("Leave request is not in a pending state.");
        }

        UserResponseDTO applicant = userInfoClient.getUserById(leave.getUserId());
        if (applicant == null) {
            throw new IllegalArgumentException("Applicant not found for leave request.");
        }

        if (!isApproverAuthorized(approver, applicant.getDepartmentId())) {
            throw new SecurityException("Approver is not authorized for this leave request.");
        }

        leave.setStatus(Leave.LeaveStatus.REJECTED);
        leave.setApproverId(approver.getId());
        leave.setComments(request.getComments());
        leave = leaveRepository.save(leave);

        LeaveBalance balance = leaveBalanceRepository
                .findByUserIdAndLeaveTypeAndYear(leave.getUserId(), leave.getLeaveType(),
                        java.time.LocalDate.now().getYear())
                .orElseThrow(() -> new IllegalStateException("Could not find leave balance to refund."));
        balance.setRemainingDays(balance.getRemainingDays() + leave.getTotalDays());
        leaveBalanceRepository.save(balance);

        String message = String.format("Your leave request for %s has been rejected.", leave.getLeaveType().getName());
        inAppNotificationService.sendNotification(applicant.getId(), message, "LEAVE_STATUS", leave.getId(),
                "/leave/history");

        return toLeaveResponse(leave);
    }

    @Override
    @Transactional
    public LeaveResponse cancelLeave(Long leaveId, String userEmail) {
        UserResponseDTO user = userInfoClient.getUserByEmail(userEmail);
        if (user == null) {
            throw new IllegalArgumentException("User not found with email: " + userEmail);
        }

        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new IllegalArgumentException("Leave application not found"));

        if (!leave.getUserId().equals(user.getId())) {
            throw new SecurityException("User is not authorized to cancel this leave application.");
        }

        if (leave.getStatus() != Leave.LeaveStatus.PENDING) {
            throw new IllegalStateException("Only pending leave requests can be cancelled.");
        }

        leave.setStatus(Leave.LeaveStatus.CANCELLED);
        leaveRepository.save(leave);

        LeaveBalance balance = leaveBalanceRepository
                .findByUserIdAndLeaveTypeAndYear(leave.getUserId(), leave.getLeaveType(),
                        java.time.LocalDate.now().getYear())
                .orElseThrow(() -> new IllegalStateException("Could not find leave balance to refund."));
        balance.setRemainingDays(balance.getRemainingDays() + leave.getTotalDays());
        leaveBalanceRepository.save(balance);

        return toLeaveResponse(leave);
    }

    @Override
    public LeaveResponse getLeaveById(Long leaveId) {
        Leave leave = leaveRepository.findById(leaveId).orElseThrow(() -> new RuntimeException("Leave not found"));
        return toLeaveResponse(leave);
    }

    @Override
    public List<LeaveResponse> getLeavesByUser(String userEmail) {
        UserResponseDTO user = userInfoClient.getUserByEmail(userEmail);
        if (user == null) {
            throw new IllegalArgumentException("User not found with email: " + userEmail);
        }
        return leaveRepository.findByUserId(user.getId()).stream()
                .map(this::toLeaveResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<LeaveResponse> getPendingLeaves(String managerEmail) {
        UserResponseDTO manager = userInfoClient.getUserByEmail(managerEmail);
        if (manager == null) {
            throw new IllegalArgumentException("Manager not found");
        }

        if (manager.getRole() == Role.ADMIN) {
            return leaveRepository.findByStatus(Leave.LeaveStatus.PENDING)
                    .stream()
                    .map(this::toLeaveResponse)
                    .collect(Collectors.toList());
        } else if (manager.getRole() == Role.MANAGER) {
            List<Long> managedDepartmentIds = userInfoClient.getDepartmentsManaged(manager.getId());
            if (managedDepartmentIds == null || managedDepartmentIds.isEmpty()) {
                return Collections.emptyList();
            }
            return leaveRepository.findByDepartmentIdInAndStatus(managedDepartmentIds, Leave.LeaveStatus.PENDING)
                    .stream()
                    .map(this::toLeaveResponse)
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<LeaveResponse> getTeamCalendar(Long departmentId, String month) {
        try {
            YearMonth yearMonth = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
            LocalDate startDate = yearMonth.atDay(1);
            LocalDate endDate = yearMonth.atEndOfMonth();

            return leaveRepository.findByDepartmentIdAndStartDateGreaterThanEqualAndEndDateLessThanEqualWithType(
                    departmentId, startDate, endDate)
                    .stream()
                    .map(this::toLeaveResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error parsing month format: {}", month, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<LeaveResponse> getTeamCalendarForManager(String managerEmail, String month) {
        UserResponseDTO manager = userInfoClient.getUserByEmail(managerEmail);
        if (manager == null) {
            throw new IllegalArgumentException("Manager not found");
        }

        if (manager.getRole() == Role.ADMIN) {
            // Admin sees all departments
            try {
                YearMonth yearMonth = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
                LocalDate startDate = yearMonth.atDay(1);
                LocalDate endDate = yearMonth.atEndOfMonth();

                return leaveRepository
                        .findByStartDateGreaterThanEqualAndEndDateLessThanEqualWithType(startDate, endDate)
                        .stream()
                        .map(this::toLeaveResponse)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                log.error("Error parsing month format: {}", month, e);
                return Collections.emptyList();
            }
        } else if (manager.getRole() == Role.MANAGER) {
            List<Long> managedDepartmentIds = userInfoClient.getDepartmentsManaged(manager.getId());
            if (managedDepartmentIds == null || managedDepartmentIds.isEmpty()) {
                return Collections.emptyList();
            }

            try {
                YearMonth yearMonth = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
                LocalDate startDate = yearMonth.atDay(1);
                LocalDate endDate = yearMonth.atEndOfMonth();

                return leaveRepository.findByDepartmentIdInAndStartDateGreaterThanEqualAndEndDateLessThanEqualWithType(
                        managedDepartmentIds, startDate, endDate)
                        .stream()
                        .map(this::toLeaveResponse)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                log.error("Error parsing month format: {}", month, e);
                return Collections.emptyList();
            }
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<LeaveResponse> getStaffTeamCalendar(Long userId, String month) {
        UserResponseDTO user = userInfoClient.getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        return getTeamCalendar(user.getDepartmentId(), month);
    }

    @Override
    public List<LeaveResponse> searchLeaves(String query) {
        // This is a simplified search implementation
        // In a real application, you might want to use a more sophisticated search
        // engine
        return leaveRepository.findAll().stream()
                .filter(leave -> {
                    try {
                        UserResponseDTO user = userInfoClient.getUserById(leave.getUserId());
                        String userName = user != null ? (user.getFirstName() + " " + user.getLastName()) : "";
                        String leaveTypeName = leave.getLeaveType() != null ? leave.getLeaveType().getName() : "";
                        String reason = leave.getReason() != null ? leave.getReason() : "";

                        return userName.toLowerCase().contains(query.toLowerCase()) ||
                                leaveTypeName.toLowerCase().contains(query.toLowerCase()) ||
                                reason.toLowerCase().contains(query.toLowerCase());
                    } catch (Exception e) {
                        log.warn("Error searching leave with ID {}: {}", leave.getId(), e.getMessage());
                        return false;
                    }
                })
                .map(this::toLeaveResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<LeaveResponse> getRecentLeaves() {
        return leaveRepository.findAll().stream()
                .limit(10) // Default limit of 10 recent leaves
                .map(this::toLeaveResponse)
                .collect(Collectors.toList());
    }

    @Override
    public int countAllLeaves() {
        return (int) leaveRepository.count();
    }

    @Override
    public int countLeavesByStatus(String status) {
        try {
            return (int) leaveRepository.countByStatus(Leave.LeaveStatus.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            log.warn("Attempted to count leaves with invalid status: {}", status);
            return 0;
        }
    }

    @Override
    public List<LeaveResponse> getLeavesByUserId(Long userId) {
        return leaveRepository.findByUserId(userId).stream()
                .map(this::toLeaveResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<LeaveResponse> getAllLeaves() {
        return leaveRepository.findAll().stream()
                .map(this::toLeaveResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<LeaveResponse> getLeavesByUserIds(List<Long> userIds) {
        return leaveRepository.findByUserIdsWithType(userIds).stream()
                .map(this::toLeaveResponse)
                .collect(Collectors.toList());
    }

    private LeaveResponse toLeaveResponse(Leave leave) {
        LeaveResponse dto = new LeaveResponse();
        dto.setId(leave.getId());
        dto.setUserId(leave.getUserId());
        dto.setLeaveTypeId(leave.getLeaveType().getId());
        dto.setLeaveTypeName(leave.getLeaveType().getName());
        dto.setStartDate(leave.getStartDate());
        dto.setEndDate(leave.getEndDate());
        dto.setReason(leave.getReason());
        dto.setStatus(leave.getStatus().name());
        dto.setComments(leave.getComments());
        dto.setApproverId(leave.getApproverId());
        dto.setCreatedAt(leave.getCreatedAt());
        dto.setUpdatedAt(leave.getUpdatedAt());

        if (leave.getDocuments() != null && !leave.getDocuments().isEmpty()) {
            dto.setDocumentIds(leave.getDocuments().stream().map(Document::getId)
                    .collect(Collectors.toList()));
            dto.setDocuments(leave.getDocuments().stream()
                    .map(documentService::toResponse)
                    .collect(Collectors.toList()));
        }

        try {
            UserResponseDTO user = userInfoClient.getUserById(leave.getUserId());
            if (user != null) {
                dto.setEmployeeName(user.getFirstName() + " " + user.getLastName());
            }
        } catch (Exception e) {
            log.warn("Could not enrich leave response with user/approver details for leaveId {}: {}", leave.getId(),
                    e.getMessage());
        }

        return dto;
    }

    private boolean isApproverAuthorized(UserResponseDTO approver, Long departmentId) {
        if (approver.getRole() == Role.ADMIN) {
            return true;
        }
        if (approver.getRole() == Role.MANAGER) {
            List<Long> managedDepts = userInfoClient.getDepartmentsManaged(approver.getId());
            return managedDepts != null && managedDepts.contains(departmentId);
        }
        return false;
    }
}