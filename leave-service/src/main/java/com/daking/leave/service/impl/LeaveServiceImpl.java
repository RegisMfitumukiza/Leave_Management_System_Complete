package com.daking.leave.service.impl;

import com.daking.leave.dto.request.LeaveApplicationRequest;
import com.daking.leave.dto.request.LeaveApprovalRequest;
import com.daking.leave.dto.response.LeaveResponse;
import com.daking.leave.model.Leave;
import com.daking.leave.model.LeaveType;
import com.daking.leave.model.LeaveBalance;
import com.daking.leave.repository.LeaveRepository;
import com.daking.leave.repository.LeaveTypeRepository;
import com.daking.leave.repository.LeaveBalanceRepository;
import com.daking.leave.service.interfaces.LeaveService;
import com.daking.leave.client.UserInfoClient;
import com.daking.leave.service.interfaces.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.util.ArrayList;
import com.daking.leave.security.ServiceAccountTokenProvider;
import com.daking.auth.api.dto.UserResponseDTO;
import com.daking.leave.service.interfaces.DocumentService;

@Service
public class LeaveServiceImpl implements LeaveService {
    private final LeaveRepository leaveRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final UserInfoClient userInfoClient;
    private final NotificationService notificationService;
    private final ServiceAccountTokenProvider serviceAccountTokenProvider;
    private final DocumentService documentService;
    private static final Logger logger = LoggerFactory.getLogger(LeaveServiceImpl.class);

    @Autowired
    public LeaveServiceImpl(LeaveRepository leaveRepository,
            LeaveTypeRepository leaveTypeRepository,
            LeaveBalanceRepository leaveBalanceRepository,
            UserInfoClient userInfoClient,
            NotificationService notificationService,
            ServiceAccountTokenProvider serviceAccountTokenProvider,
            DocumentService documentService) {
        this.leaveRepository = leaveRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.userInfoClient = userInfoClient;
        this.notificationService = notificationService;
        this.serviceAccountTokenProvider = serviceAccountTokenProvider;
        this.documentService = documentService;
    }

    @Override
    @Transactional
    public LeaveResponse applyForLeave(String userIdStr, LeaveApplicationRequest request) {
        try {
            // Validate user (via Feign)
            Long userId = Long.parseLong(userIdStr);
            var user = userInfoClient.getUserById(userId, null);
            if (user == null)
                throw new IllegalArgumentException("User not found");

            // Validate leave type
            LeaveType leaveType = leaveTypeRepository.findById(request.getLeaveTypeId())
                    .orElseThrow(() -> new IllegalArgumentException("Leave type not found"));
            if (!leaveType.getIsActive())
                throw new IllegalArgumentException("Leave type is not active");

            // Validate dates
            LocalDate today = LocalDate.now();
            if (request.getStartDate().isBefore(today))
                throw new IllegalArgumentException("Start date cannot be in the past");
            if (request.getEndDate().isBefore(request.getStartDate()))
                throw new IllegalArgumentException("End date cannot be before start date");

            // Calculate total days
            double totalDays = (double) (request.getEndDate().toEpochDay() - request.getStartDate().toEpochDay() + 1);
            if (totalDays <= 0)
                throw new IllegalArgumentException("Invalid leave duration");

            // Check leave balance
            int year = today.getYear();
            LeaveBalance balance = leaveBalanceRepository.findByUserIdAndLeaveTypeAndYear(userId, leaveType, year)
                    .orElseGet(() -> {
                        LeaveBalance b = new LeaveBalance();
                        b.setUserId(userId);
                        b.setLeaveType(leaveType);
                        b.setYear(year);
                        b.setTotalDays(leaveType.getDefaultDays());
                        b.setUsedDays(0.0);
                        b.setRemainingDays(leaveType.getDefaultDays());
                        b.setCarriedOverDays(0.0);
                        return b;
                    });
            if (balance.getRemainingDays() < totalDays)
                throw new IllegalArgumentException("Insufficient leave balance");

            // Debug log for documentIds
            logger.info("Received documentIds in leave application: {}", request.getDocumentIds());

            // Create Leave entity
            Leave leave = new Leave();
            leave.setUserId(userId);
            leave.setLeaveType(leaveType);
            leave.setStartDate(request.getStartDate());
            leave.setEndDate(request.getEndDate());
            leave.setTotalDays(totalDays);
            leave.setStatus(Leave.LeaveStatus.PENDING);
            leave.setReason(request.getReason());
            // Always set documentIds, even if null or empty
            leave.setDocumentIds(request.getDocumentIds() != null && !request.getDocumentIds().isEmpty()
                    ? request.getDocumentIds().stream().map(String::valueOf).collect(Collectors.joining(","))
                    : null);
            leave.setDepartmentId(user.getDepartmentId());
            leave = leaveRepository.save(leave);

            // Reserve leave days (deduct from balance)
            balance.setUsedDays(balance.getUsedDays() + totalDays);
            balance.setRemainingDays(balance.getRemainingDays() - totalDays);
            leaveBalanceRepository.save(balance);

            // Notify user (email)
            notificationService.sendEmail(user.getEmail(), "Leave Application Submitted",
                    "Your leave application has been submitted.");
            notificationService.sendInAppNotification(userId, "Your leave application has been submitted.",
                    "LEAVE_SUBMITTED", leave.getId(), "/staff/leave-details/" + leave.getId());

            return toResponse(leave);
        } catch (Exception e) {
            logger.error("Error applying for leave", e);
            throw e;
        }
    }

    private LeaveResponse toResponse(Leave leave) {
        LeaveType leaveType = leave.getLeaveType();
        LeaveResponse dto = new LeaveResponse();
        dto.setId(leave.getId());
        dto.setUserId(leave.getUserId());
        dto.setLeaveTypeId(leaveType.getId());
        dto.setLeaveTypeName(leaveType.getName());
        dto.setStartDate(leave.getStartDate());
        dto.setEndDate(leave.getEndDate());
        dto.setTotalDays(leave.getTotalDays());
        dto.setStatus(leave.getStatus().name());
        dto.setReason(leave.getReason());
        dto.setComments(leave.getComments());
        dto.setApproverId(leave.getApproverId());
        if (leave.getDocumentIds() != null && !leave.getDocumentIds().isEmpty()) {
            List<Long> docIds = java.util.Arrays.stream(leave.getDocumentIds().split(",")).map(Long::valueOf)
                    .collect(Collectors.toList());
            dto.setDocumentIds(docIds);
            List<com.daking.leave.dto.response.DocumentResponse> docs = docIds.stream()
                    .map(id -> {
                        try {
                            return documentService.getDocumentById(id);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
            dto.setDocuments(docs);
        }
        dto.setCreatedAt(leave.getCreatedAt());
        dto.setUpdatedAt(leave.getUpdatedAt());
        // Set employeeName and leaveType for frontend display
        try {
            String systemToken = serviceAccountTokenProvider.getToken();
            var user = userInfoClient.getUserById(leave.getUserId(), "Bearer " + systemToken);
            if (user != null) {
                dto.setEmployeeName(user.getFirstName() + " " + user.getLastName());
            } else {
                dto.setEmployeeName("User " + leave.getUserId());
            }
        } catch (Exception e) {
            dto.setEmployeeName("User " + leave.getUserId());
        }
        dto.setLeaveType(leaveType.getName());
        return dto;
    }

    @Override
    @Transactional
    public LeaveResponse approveLeave(Long leaveId, String approverEmail, LeaveApprovalRequest request) {
        try {
            String systemToken = serviceAccountTokenProvider.getToken();
            Object approverObj = approverEmail != null && approverEmail.matches("\\d+")
                    ? userInfoClient.getUserById(Long.parseLong(approverEmail), systemToken)
                    : userInfoClient.getUserByEmail(approverEmail, systemToken);
            if (approverObj == null)
                throw new IllegalArgumentException("Approver not found");
            Long approverId = (approverObj instanceof com.daking.auth.api.model.User)
                    ? ((com.daking.auth.api.model.User) approverObj).getId()
                    : ((com.daking.auth.api.dto.UserResponseDTO) approverObj).getId();
            Leave leave = leaveRepository.findById(leaveId)
                    .orElseThrow(() -> new IllegalArgumentException("Leave application not found"));
            if (leave.getStatus() != Leave.LeaveStatus.PENDING) {
                throw new IllegalStateException("Only pending leave can be approved");
            }
            // Validate approver is manager/admin for the applicant's department
            var applicant = userInfoClient.getUserById(leave.getUserId(), systemToken);
            if (applicant == null)
                throw new IllegalArgumentException("Applicant not found");
            String approverRole = (approverObj instanceof com.daking.auth.api.model.User)
                    ? ((com.daking.auth.api.model.User) approverObj).getRole().name()
                    : ((com.daking.auth.api.dto.UserResponseDTO) approverObj).getRole().name();
            if (!approverRole.equals("ADMIN")) {
                // Fetch all departments managed by this manager
                java.util.List<Long> managedDepartments = userInfoClient.getDepartmentsManaged(approverId,
                        systemToken);
                if (managedDepartments == null || !managedDepartments.contains(applicant.getDepartmentId())) {
                    throw new IllegalArgumentException("Approver is not authorized for this leave");
                }
            }
            leave.setStatus(Leave.LeaveStatus.APPROVED);
            leave.setApproverId(approverId);
            leave.setComments(request.getComments());
            leave = leaveRepository.save(leave);

            // Notify applicant (email)
            notificationService.sendEmail(applicant.getEmail(), "Leave Approved", "Your leave has been approved.");
            notificationService.sendInAppNotification(leave.getUserId(), "Your leave has been approved.",
                    "LEAVE_APPROVED", leave.getId(), "/staff/leave-details/" + leave.getId());

            return toResponse(leave);
        } catch (Exception e) {
            logger.error("Error approving leave", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public LeaveResponse rejectLeave(Long leaveId, String approverEmail, LeaveApprovalRequest request) {
        try {
            String systemToken = serviceAccountTokenProvider.getToken();
            Object approverObj = approverEmail != null && approverEmail.matches("\\d+")
                    ? userInfoClient.getUserById(Long.parseLong(approverEmail), systemToken)
                    : userInfoClient.getUserByEmail(approverEmail, systemToken);
            if (approverObj == null)
                throw new IllegalArgumentException("Approver not found");
            Long approverId = (approverObj instanceof com.daking.auth.api.model.User)
                    ? ((com.daking.auth.api.model.User) approverObj).getId()
                    : ((com.daking.auth.api.dto.UserResponseDTO) approverObj).getId();

            Leave leave = leaveRepository.findById(leaveId)
                    .orElseThrow(() -> new IllegalArgumentException("Leave application not found"));
            if (leave.getStatus() != Leave.LeaveStatus.PENDING) {
                throw new IllegalStateException("Only pending leave can be rejected");
            }
            // Validate approver is manager/admin for the applicant's department
            var applicant = userInfoClient.getUserById(leave.getUserId(), systemToken);
            if (applicant == null)
                throw new IllegalArgumentException("Applicant not found");
            String approverRole = (approverObj instanceof com.daking.auth.api.model.User)
                    ? ((com.daking.auth.api.model.User) approverObj).getRole().name()
                    : ((com.daking.auth.api.dto.UserResponseDTO) approverObj).getRole().name();
            if (!approverRole.equals("ADMIN")) {
                // Fetch all departments managed by this manager
                java.util.List<Long> managedDepartments = userInfoClient.getDepartmentsManaged(approverId,
                        systemToken);
                if (managedDepartments == null || !managedDepartments.contains(applicant.getDepartmentId())) {
                    throw new IllegalArgumentException("Approver is not authorized for this leave");
                }
            }
            leave.setStatus(Leave.LeaveStatus.REJECTED);
            leave.setApproverId(approverId);
            leave.setComments(request.getComments());
            leave = leaveRepository.save(leave);
            // Restore leave days to balance
            LeaveType leaveType = leave.getLeaveType();
            int year = leave.getStartDate().getYear();
            LeaveBalance balance = leaveBalanceRepository
                    .findByUserIdAndLeaveTypeAndYear(leave.getUserId(), leaveType, year)
                    .orElseThrow(() -> new IllegalStateException("Leave balance not found"));
            balance.setUsedDays(balance.getUsedDays() - leave.getTotalDays());
            balance.setRemainingDays(balance.getRemainingDays() + leave.getTotalDays());
            leaveBalanceRepository.save(balance);

            // Notify applicant (email)
            notificationService.sendEmail(applicant.getEmail(), "Leave Rejected", "Your leave has been rejected.");
            notificationService.sendInAppNotification(leave.getUserId(), "Your leave has been rejected.");

            return toResponse(leave);
        } catch (Exception e) {
            logger.error("Error rejecting leave", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public LeaveResponse cancelLeave(Long leaveId, String userEmail) {
        String systemToken = serviceAccountTokenProvider.getToken();
        var user = userInfoClient.getUserByEmail(userEmail, systemToken);
        if (user == null)
            throw new IllegalArgumentException("User not found");
        Long userId = user.getId();
        // Fetch leave
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new IllegalArgumentException("Leave application not found"));
        if (leave.getUserId() == null || !leave.getUserId().equals(userId)) {
            throw new IllegalArgumentException("User is not the owner of this leave application");
        }
        if (leave.getStatus() != Leave.LeaveStatus.PENDING && leave.getStatus() != Leave.LeaveStatus.APPROVED) {
            throw new IllegalStateException("Only pending or approved leave can be cancelled");
        }
        // Update leave status
        leave.setStatus(Leave.LeaveStatus.CANCELLED);
        leave = leaveRepository.save(leave);
        // Restore leave days to balance
        LeaveType leaveType = leave.getLeaveType();
        int year = leave.getStartDate().getYear();
        LeaveBalance balance = leaveBalanceRepository
                .findByUserIdAndLeaveTypeAndYear(leave.getUserId(), leaveType, year)
                .orElseThrow(() -> new IllegalStateException("Leave balance not found"));
        balance.setUsedDays(balance.getUsedDays() - leave.getTotalDays());
        balance.setRemainingDays(balance.getRemainingDays() + leave.getTotalDays());
        leaveBalanceRepository.save(balance);
        return toResponse(leave);
    }

    @Override
    public LeaveResponse getLeaveById(Long leaveId) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new IllegalArgumentException("Leave application not found"));
        // If you need user info here, use systemToken
        return toResponse(leave);
    }

    @Override
    public List<LeaveResponse> getLeavesByUser(String userEmail) {
        String systemToken = serviceAccountTokenProvider.getToken();
        var user = userInfoClient.getUserByEmail(userEmail, systemToken);
        if (user == null)
            throw new IllegalArgumentException("User not found");
        Long userId = user.getId();
        List<Leave> leaves = leaveRepository.findByUserIdWithType(userId);
        return leaves.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<LeaveResponse> getPendingLeaves(String managerEmail) {
        String systemToken = serviceAccountTokenProvider.getToken();
        Object managerObj = managerEmail != null && managerEmail.matches("\\d+")
                ? userInfoClient.getUserById(Long.parseLong(managerEmail), systemToken)
                : userInfoClient.getUserByEmail(managerEmail, systemToken);
        if (managerObj == null)
            throw new IllegalArgumentException("Manager not found");
        Long managerId = (managerObj instanceof com.daking.auth.api.model.User)
                ? ((com.daking.auth.api.model.User) managerObj).getId()
                : ((com.daking.auth.api.dto.UserResponseDTO) managerObj).getId();
        List<Long> departmentIds = userInfoClient.getDepartmentsManaged(managerId, systemToken);
        if (departmentIds == null || departmentIds.isEmpty()) {
            return List.of();
        }
        List<Long> teamUserIds = new java.util.ArrayList<>();
        for (Long departmentId : departmentIds) {
            List<UserResponseDTO> team = userInfoClient.getTeamMembers(departmentId, systemToken);
            if (team != null) {
                teamUserIds.addAll(team.stream().map(UserResponseDTO::getId).toList());
            }
        }
        return leaveRepository.findByStatusWithType(Leave.LeaveStatus.PENDING).stream()
                .filter(l -> teamUserIds.contains(l.getUserId()))
                .map(leave -> {
                    LeaveType leaveType = leave.getLeaveType();
                    if (leaveType != null) {
                        leaveType.getName();
                        leaveType.getId();
                    }
                    return toResponse(leave);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveResponse> getTeamCalendar(Long departmentId, String month) {
        String systemToken = serviceAccountTokenProvider.getToken();
        List<UserResponseDTO> team = userInfoClient.getTeamMembers(departmentId, systemToken);
        List<Long> teamUserIds = team.stream().map(UserResponseDTO::getId).toList();
        List<Leave> allLeaves = leaveRepository.findByUserIdsWithType(teamUserIds);
        if (month != null && !month.isEmpty()) {
            String[] parts = month.split("-");
            int year = Integer.parseInt(parts[0]);
            int monthNum = Integer.parseInt(parts[1]);
            allLeaves = allLeaves.stream()
                    .filter(l -> l.getStartDate().getYear() == year && l.getStartDate().getMonthValue() == monthNum)
                    .toList();
        }
        return allLeaves.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<LeaveResponse> searchLeaves(String query) {
        // Simple search: filter by reason or status
        String q = query == null ? "" : query.toLowerCase();
        List<Leave> filteredLeaves = leaveRepository.findAll().stream()
                .filter(l -> (l.getReason() != null && l.getReason().toLowerCase().contains(q))
                        || l.getStatus().name().toLowerCase().contains(q))
                .toList();
        return filteredLeaves.stream().map(leave -> {
            LeaveType leaveType = leave.getLeaveType();
            if (leaveType != null) {
                leaveType.getName();
                leaveType.getId();
            }
            return toResponse(leave);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveResponse> getTeamCalendarForManager(String managerEmail, String month) {
        Object managerObj = managerEmail != null && managerEmail.matches("\\d+")
                ? userInfoClient.getUserById(Long.parseLong(managerEmail), null)
                : userInfoClient.getUserByEmail(managerEmail, null);
        if (managerObj == null)
            throw new IllegalArgumentException("Manager not found");
        Long managerId = (managerObj instanceof com.daking.auth.api.model.User)
                ? ((com.daking.auth.api.model.User) managerObj).getId()
                : ((com.daking.auth.api.dto.UserResponseDTO) managerObj).getId();
        String systemToken = serviceAccountTokenProvider.getToken();
        List<Long> departmentIds = userInfoClient.getDepartmentsManaged(managerId, systemToken);
        if ((departmentIds == null || departmentIds.isEmpty()) && "ADMIN".equalsIgnoreCase(
                (managerObj instanceof com.daking.auth.api.model.User)
                        ? ((com.daking.auth.api.model.User) managerObj).getRole().name()
                        : ((com.daking.auth.api.dto.UserResponseDTO) managerObj).getRole().name())) {
            List<Leave> allLeaves = leaveRepository.findAllWithType();
            if (month != null && !month.isEmpty()) {
                String[] parts = month.split("-");
                int year = Integer.parseInt(parts[0]);
                int monthNum = Integer.parseInt(parts[1]);
                allLeaves = allLeaves.stream()
                        .filter(l -> l.getStartDate().getYear() == year && l.getStartDate().getMonthValue() == monthNum)
                        .toList();
            }
            return allLeaves.stream().map(this::toResponse).collect(Collectors.toList());
        }
        if (departmentIds == null || departmentIds.isEmpty()) {
            throw new IllegalArgumentException("Manager does not manage any departments");
        }
        List<Long> teamUserIds = new ArrayList<>();
        for (Long departmentId : departmentIds) {
            List<UserResponseDTO> team = userInfoClient.getTeamMembers(departmentId, systemToken);
            teamUserIds.addAll(team.stream().map(UserResponseDTO::getId).toList());
        }
        List<Leave> allLeaves = leaveRepository.findByUserIdsWithType(teamUserIds);
        if (month != null && !month.isEmpty()) {
            String[] parts = month.split("-");
            int year = Integer.parseInt(parts[0]);
            int monthNum = Integer.parseInt(parts[1]);
            allLeaves = allLeaves.stream()
                    .filter(l -> l.getStartDate().getYear() == year && l.getStartDate().getMonthValue() == monthNum)
                    .toList();
        }
        return allLeaves.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveResponse> getRecentLeaves() {
        try {
            List<Leave> recentLeaves = leaveRepository.findAllWithType().stream()
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .limit(10)
                    .toList();
            return recentLeaves.stream().map(this::toResponse).collect(Collectors.toList());
        } catch (Exception ex) {
            logger.error("Error in getRecentLeaves: {}", ex.getMessage());
            return List.of();
        }
    }

    @Override
    public int countAllLeaves() {
        try {
            return (int) leaveRepository.count();
        } catch (Exception ex) {
            logger.error("Error in countAllLeaves: {}", ex.getMessage());
            return 0;
        }
    }

    @Override
    public int countLeavesByStatus(String status) {
        try {
            return (int) leaveRepository.findAll().stream()
                    .filter(l -> l.getStatus().name().equalsIgnoreCase(status))
                    .count();
        } catch (Exception ex) {
            logger.error("Error in countLeavesByStatus: {}", ex.getMessage());
            return 0;
        }
    }

    @Override
    public List<LeaveResponse> getLeavesByUserId(Long userId) {
        List<Leave> leaves = leaveRepository.findByUserIdWithType(userId);
        return leaves.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveResponse> getAllLeaves() {
        List<Leave> leaves = leaveRepository.findAllWithType();
        return leaves.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<LeaveResponse> getStaffTeamCalendar(Long userId, String month) {
        // userId is the staff's userId (see JwtAuthenticationFilter)
        String systemToken = serviceAccountTokenProvider.getToken();
        var staff = userInfoClient.getUserById(userId, systemToken);
        if (staff == null)
            throw new IllegalArgumentException("Staff not found");

        // Get staff's department
        Long departmentId = staff.getDepartmentId();
        if (departmentId == null)
            throw new IllegalArgumentException("Staff is not assigned to any department");

        // Get team members for the department
        List<UserResponseDTO> team = userInfoClient.getTeamMembers(departmentId, systemToken);
        List<Long> teamUserIds = team.stream().map(UserResponseDTO::getId).toList();

        // Get all leaves for team members (eager fetch leaveType)
        List<Leave> allLeaves = leaveRepository.findByUserIdsWithType(teamUserIds);

        // Filter by month if specified
        if (month != null && !month.isEmpty()) {
            String[] parts = month.split("-");
            int year = Integer.parseInt(parts[0]);
            int monthNum = Integer.parseInt(parts[1]);
            allLeaves = allLeaves.stream()
                    .filter(l -> l.getStartDate().getYear() == year && l.getStartDate().getMonthValue() == monthNum)
                    .toList();
        }

        return allLeaves.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<LeaveResponse> getLeavesByUserIds(List<Long> userIds) {
        List<Leave> leaves = leaveRepository.findByUserIdsWithType(userIds);
        return leaves.stream().map(this::toResponse).collect(Collectors.toList());
    }
}