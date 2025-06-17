package com.daking.leave.controller;

import com.daking.leave.repository.LeaveRepository;
import com.daking.leave.repository.LeaveBalanceRepository;
import com.daking.leave.client.UserInfoClient;
import com.daking.auth.api.model.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;
import com.daking.leave.security.ServiceAccountTokenProvider;

@RestController
@RequestMapping("/api/leave-analytics")
public class LeaveAnalyticsController {
    private final LeaveRepository leaveRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final UserInfoClient userInfoClient;
    private final ServiceAccountTokenProvider serviceAccountTokenProvider;

    public LeaveAnalyticsController(LeaveRepository leaveRepository, LeaveBalanceRepository leaveBalanceRepository,
            UserInfoClient userInfoClient,
            ServiceAccountTokenProvider serviceAccountTokenProvider) {
        this.leaveRepository = leaveRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.userInfoClient = userInfoClient;
        this.serviceAccountTokenProvider = serviceAccountTokenProvider;
    }

    @GetMapping("/department-distribution")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Map<String, Object>> getDepartmentDistribution(@RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long departmentId) {
        String systemToken = serviceAccountTokenProvider.getToken();
        List<User> users = userInfoClient.getUsersByRole("STAFF", systemToken);
        Map<String, Object> result = new HashMap<>();
        if (users == null || users.isEmpty())
            return ResponseEntity.ok(result);
        for (User user : users) {
            if (departmentId != null
                    && (user.getDepartmentId() == null || !user.getDepartmentId().equals(departmentId)))
                continue;
            String deptName = (user.getDepartmentId() != null) ? ("Department " + user.getDepartmentId()) : "Unknown";
            List<com.daking.leave.model.LeaveBalance> balances = leaveBalanceRepository.findByUserId(user.getId());
            double total = 0, used = 0, remaining = 0;
            for (com.daking.leave.model.LeaveBalance b : balances) {
                if (year == null || b.getYear() == year) {
                    total += b.getTotalDays();
                    used += b.getUsedDays();
                    remaining += b.getRemainingDays();
                }
            }
            Object statsObj = result.getOrDefault(deptName, new HashMap<String, Object>());
            @SuppressWarnings("unchecked")
            Map<String, Object> stats = (statsObj instanceof Map) ? (Map<String, Object>) statsObj : new HashMap<>();
            stats.put("usedDays", ((double) stats.getOrDefault("usedDays", 0.0)) + used);
            stats.put("totalDays", ((double) stats.getOrDefault("totalDays", 0.0)) + total);
            stats.put("remainingDays", ((double) stats.getOrDefault("remainingDays", 0.0)) + remaining);
            result.put(deptName, stats);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/usage-trends")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Map<String, Object>> getUsageTrends(@RequestParam Integer year,
            @RequestParam String interval) {
        Map<String, Object> result = new HashMap<>();
        List<com.daking.leave.model.Leave> leaves = leaveRepository.findAllWithType();
        if ("MONTHLY".equalsIgnoreCase(interval)) {
            for (int m = 1; m <= 12; m++) {
                int month = m;
                double sum = leaves.stream()
                        .filter(l -> l.getStartDate().getYear() == year && l.getStartDate().getMonthValue() == month)
                        .mapToDouble(com.daking.leave.model.Leave::getTotalDays).sum();
                result.put(java.time.Month.of(month).name().substring(0, 3), sum);
            }
        } else if ("QUARTERLY".equalsIgnoreCase(interval)) {
            for (int q = 1; q <= 4; q++) {
                int startMonth = (q - 1) * 3 + 1;
                int endMonth = startMonth + 2;
                double sum = leaves.stream()
                        .filter(l -> l.getStartDate().getYear() == year &&
                                l.getStartDate().getMonthValue() >= startMonth &&
                                l.getStartDate().getMonthValue() <= endMonth)
                        .mapToDouble(com.daking.leave.model.Leave::getTotalDays).sum();
                result.put("Q" + q, sum);
            }
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/balance-alerts")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<Map<String, Object>>> getBalanceAlerts(
            @RequestParam(required = false) Long departmentId) {
        List<Map<String, Object>> alerts = new ArrayList<>();
        List<com.daking.leave.model.LeaveBalance> balances = leaveBalanceRepository.findAllWithType();
        String systemToken = serviceAccountTokenProvider.getToken();
        for (com.daking.leave.model.LeaveBalance b : balances) {
            if (b.getRemainingDays() < 3) {
                User user = userInfoClient.getUserById(b.getUserId(), systemToken);
                if (departmentId != null && (user == null || user.getDepartmentId() == null
                        || !user.getDepartmentId().equals(departmentId)))
                    continue;
                Map<String, Object> alert = new HashMap<>();
                alert.put("userName", user != null ? user.getFullName() : "Unknown");
                alert.put("department",
                        user != null && user.getDepartmentId() != null ? ("Department " + user.getDepartmentId())
                                : "Unknown");
                alert.put("leaveType", b.getLeaveType().getName());
                alert.put("remainingDays", b.getRemainingDays());
                alerts.add(alert);
            }
        }
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/ytd-consumption")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Map<String, Object>> getYtdConsumption(@RequestParam Integer year) {
        Map<String, Object> result = new HashMap<>();
        List<com.daking.leave.model.Leave> leaves = leaveRepository.findAllWithType();
        leaves.stream()
                .filter(l -> l.getStartDate().getYear() == year)
                .collect(Collectors.groupingBy(l -> l.getLeaveType().getName(),
                        Collectors.summingDouble(com.daking.leave.model.Leave::getTotalDays)))
                .forEach(result::put);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/carryover-stats")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Map<String, Object>> getCarryoverStats(@RequestParam Integer year) {
        Map<String, Object> result = new HashMap<>();
        List<com.daking.leave.model.LeaveBalance> balances = leaveBalanceRepository.findAllWithType();
        String systemToken = serviceAccountTokenProvider.getToken();
        for (com.daking.leave.model.LeaveBalance b : balances) {
            if (b.getYear() != year)
                continue;
            User user = userInfoClient.getUserById(b.getUserId(), systemToken);
            String dept = user != null && user.getDepartmentId() != null ? ("Department " + user.getDepartmentId())
                    : "Unknown";
            double carry = b.getCarriedOverDays();
            result.put(dept, ((double) result.getOrDefault(dept, 0.0)) + carry);
        }
        return ResponseEntity.ok(result);
    }
}