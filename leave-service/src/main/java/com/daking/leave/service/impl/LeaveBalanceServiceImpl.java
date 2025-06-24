package com.daking.leave.service.impl;

import com.daking.leave.dto.request.LeaveBalanceAdjustmentRequest;
import com.daking.leave.dto.response.LeaveBalanceResponse;
import com.daking.leave.model.LeaveBalance;
import com.daking.leave.model.LeaveType;
import com.daking.leave.repository.LeaveBalanceRepository;
import com.daking.leave.repository.LeaveTypeRepository;
import com.daking.leave.service.interfaces.LeaveBalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.daking.leave.service.SettingsService;
import com.daking.leave.service.interfaces.NotificationService;
import com.daking.leave.client.UserInfoClient;
import com.daking.auth.api.dto.UserResponseDTO;
import com.daking.auth.api.model.Role;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class LeaveBalanceServiceImpl implements LeaveBalanceService {
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final SettingsService settingsService;
    private final NotificationService notificationService;
    private final UserInfoClient userInfoClient;

    @Override
    public List<LeaveBalanceResponse> getLeaveBalancesByUser(Long userId) {
        // Check if user is admin
        try {
            String role = userInfoClient.getUserRole(userId);
            if (Role.ADMIN.name().equals(role)) {
                return List.of(); // Admins have no leave balances
            }
        } catch (Exception e) {
            log.warn("Could not verify user role for userId {}: {}", userId, e.getMessage());
        }

        return leaveBalanceRepository.findByUserIdWithType(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public LeaveBalanceResponse adjustLeaveBalance(LeaveBalanceAdjustmentRequest request) {
        // Check if user is admin
        try {
            String role = userInfoClient.getUserRole(request.getUserId());
            if (Role.ADMIN.name().equals(role)) {
                throw new IllegalArgumentException("Cannot adjust leave balance for admin user");
            }
        } catch (Exception e) {
            log.warn("Could not verify user role for userId {}: {}", request.getUserId(), e.getMessage());
        }

        LeaveType leaveType = leaveTypeRepository.findById(request.getLeaveTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Leave type not found"));
        int year = LocalDate.now().getYear();
        LeaveBalance balance = leaveBalanceRepository
                .findByUserIdAndLeaveTypeAndYear(request.getUserId(), leaveType, year)
                .orElseGet(() -> {
                    LeaveBalance b = new LeaveBalance();
                    b.setUserId(request.getUserId());
                    b.setLeaveType(leaveType);
                    b.setYear(year);
                    b.setTotalDays(leaveType.getDefaultDays());
                    b.setUsedDays(0.0);
                    b.setRemainingDays(leaveType.getDefaultDays());
                    b.setCarriedOverDays(0.0);
                    return b;
                });
        // Adjust balance
        balance.setTotalDays(balance.getTotalDays() + request.getAdjustmentDays());
        balance.setRemainingDays(balance.getRemainingDays() + request.getAdjustmentDays());
        leaveBalanceRepository.save(balance);
        return toResponse(balance);
    }

    @Override
    public List<LeaveBalanceResponse> getBulkLeaveBalances(List<Long> userIds) {
        return leaveBalanceRepository.findAll().stream()
                .filter(b -> userIds.contains(b.getUserId()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public LeaveBalanceResponse getLeaveBalance(Long userId, Long leaveTypeId, Integer year) {
        LeaveType leaveType = leaveTypeRepository.findById(leaveTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Leave type not found"));
        Optional<LeaveBalance> balance = leaveBalanceRepository.findByUserIdAndLeaveTypeAndYear(userId, leaveType,
                year);
        return balance.map(this::toResponse).orElse(null);
    }

    @Override
    public void bulkAdjustLeaveBalances(List<Long> userIds, Long leaveTypeId, int adjustmentDays, String reason) {
        for (Long userId : userIds) {
            LeaveBalanceAdjustmentRequest req = new LeaveBalanceAdjustmentRequest();
            req.setUserId(userId);
            req.setLeaveTypeId(leaveTypeId);
            req.setAdjustmentDays((double) adjustmentDays);
            req.setReason(reason);
            adjustLeaveBalance(req);
        }
    }

    private LeaveBalanceResponse toResponse(LeaveBalance balance) {
        LeaveBalanceResponse dto = new LeaveBalanceResponse();
        dto.setId(balance.getId());
        dto.setUserId(balance.getUserId());
        LeaveType leaveType = balance.getLeaveType();
        if (leaveType == null) {
            dto.setLeaveTypeName("Unknown Leave Type");
        } else {
            dto.setLeaveTypeId(leaveType.getId());
            dto.setLeaveTypeName(leaveType.getName());
        }
        dto.setYear(balance.getYear());
        dto.setTotalDays(balance.getTotalDays());
        dto.setUsedDays(balance.getUsedDays());
        dto.setRemainingDays(balance.getRemainingDays());
        dto.setCarriedOverDays(balance.getCarriedOverDays());
        return dto;
    }

    /**
     * Scheduled job to accrue leave monthly for all users and leave types.
     * Runs at midnight on the 1st of every month.
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void accrueMonthlyLeave() {
        log.info("Starting scheduled monthly leave accrual job");
        double accrualRate = settingsService.getSettings().getAccrualRate();
        int year = LocalDate.now().getYear();
        List<LeaveBalance> balances = leaveBalanceRepository.findAll();
        int updated = 0;
        for (LeaveBalance balance : balances) {
            // Skip admins
            try {
                String role = userInfoClient.getUserRole(balance.getUserId());
                if (Role.ADMIN.name().equals(role)) {
                    continue;
                }
            } catch (Exception e) {
                log.warn("Could not verify user role for userId {}: {}", balance.getUserId(), e.getMessage());
                continue;
            }

            if (balance.getYear() == year) {
                balance.setTotalDays(balance.getTotalDays() + accrualRate);
                balance.setRemainingDays(balance.getRemainingDays() + accrualRate);
                updated++;
            }
        }
        leaveBalanceRepository.saveAll(balances);
        log.info("Monthly leave accrual job completed. Updated {} balances.", updated);
        notifyAdmins("Monthly leave accrual completed. " + updated + " balances updated.");
    }

    /**
     * Scheduled job to carry over unused leave at the end of January.
     * Runs at midnight on January 31st every year.
     */
    @Scheduled(cron = "0 0 0 31 1 ?")
    @Transactional
    public void carryOverUnusedLeave() {
        log.info("Starting scheduled annual carry-over job");
        int maxCarryover = settingsService.getSettings().getMaxCarryover();
        int prevYear = LocalDate.now().getYear() - 1;
        List<LeaveBalance> lastYearBalances = leaveBalanceRepository.findByYear(prevYear);
        int updated = 0;
        for (LeaveBalance balance : lastYearBalances) {
            // Skip admins
            try {
                String role = userInfoClient.getUserRole(balance.getUserId());
                if (Role.ADMIN.name().equals(role)) {
                    continue;
                }
            } catch (Exception e) {
                log.warn("Could not verify user role for userId {}: {}", balance.getUserId(), e.getMessage());
                continue;
            }

            double carry = Math.min(balance.getRemainingDays(), maxCarryover);
            LeaveBalance thisYear = leaveBalanceRepository
                    .findByUserIdAndLeaveTypeAndYear(balance.getUserId(), balance.getLeaveType(), prevYear + 1)
                    .orElseGet(() -> {
                        LeaveBalance b = new LeaveBalance();
                        b.setUserId(balance.getUserId());
                        b.setLeaveType(balance.getLeaveType());
                        b.setYear(prevYear + 1);
                        b.setTotalDays(0.0);
                        b.setUsedDays(0.0);
                        b.setRemainingDays(0.0);
                        b.setCarriedOverDays(0.0);
                        return b;
                    });
            thisYear.setCarriedOverDays(carry);
            thisYear.setTotalDays(thisYear.getTotalDays() + carry);
            thisYear.setRemainingDays(thisYear.getRemainingDays() + carry);
            leaveBalanceRepository.save(thisYear);
            updated++;
        }
        log.info("Annual carry-over job completed. Updated {} balances.", updated);
        notifyAdmins("Annual carry-over completed. " + updated + " balances updated.");
    }

    private void notifyAdmins(String message) {
        try {
            // Fetch all admin users from auth-service using Feign interceptor
            List<UserResponseDTO> admins = userInfoClient.getUsersByRole("ADMIN");
            for (UserResponseDTO admin : admins) {
                if (admin.getId() != null) {
                    notificationService.sendInAppNotification(admin.getId(), message, "ADMIN_ALERT", null, null);
                }
            }
        } catch (Exception e) {
            log.error("Failed to notify admins: {}", e.getMessage(), e);
        }
    }

    /**
     * Ensure all users have leave balances for all active leave types for the
     * current year.
     * Only creates missing balances, does not overwrite existing ones.
     */
    @Transactional
    public int initializeMissingLeaveBalances() {
        int year = LocalDate.now().getYear();

        try {
            // Get all staff and managers (exclude admins)
            List<UserResponseDTO> users = userInfoClient.getUsersByRole("STAFF");
            users.addAll(userInfoClient.getUsersByRole("MANAGER"));

            List<LeaveType> leaveTypes = leaveTypeRepository.findAll().stream()
                    .filter(LeaveType::getIsActive)
                    .collect(Collectors.toList());

            int created = 0;
            for (UserResponseDTO user : users) {
                for (LeaveType leaveType : leaveTypes) {
                    boolean exists = leaveBalanceRepository
                            .findByUserIdAndLeaveTypeAndYear(user.getId(), leaveType, year)
                            .isPresent();
                    if (!exists) {
                        LeaveBalance b = new LeaveBalance();
                        b.setUserId(user.getId());
                        b.setLeaveType(leaveType);
                        b.setYear(year);
                        b.setTotalDays(leaveType.getDefaultDays());
                        b.setUsedDays(0.0);
                        b.setRemainingDays(leaveType.getDefaultDays());
                        b.setCarriedOverDays(0.0);
                        leaveBalanceRepository.save(b);
                        created++;
                    }
                }
            }
            log.info("Initialized {} missing leave balances for year {}", created, year);
            return created;
        } catch (Exception e) {
            log.error("Failed to initialize missing leave balances: {}", e.getMessage(), e);
            return 0;
        }
    }
}