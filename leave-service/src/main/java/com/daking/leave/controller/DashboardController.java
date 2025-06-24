package com.daking.leave.controller;

import com.daking.auth.api.dto.DepartmentDTO;
import com.daking.auth.api.dto.UserResponseDTO;
import com.daking.leave.client.UserInfoClient;
import com.daking.leave.exception.DashboardStatsException;
import com.daking.leave.model.Leave;
import com.daking.leave.repository.LeaveRepository;
import com.daking.leave.repository.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {
    private final UserInfoClient userInfoClient;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveRepository leaveRepository;

    /**
     * Get dashboard statistics for admin users
     * 
     * @return Map containing total users, departments, leave types, and pending
     *         requests
     */
    @GetMapping("/dashboard-stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        log.info("Fetching dashboard statistics for admin");

        try {
            // Get users by role (no token needed - handled by Feign client)
            List<UserResponseDTO> users = userInfoClient.getUsersByRole("STAFF");
            log.debug("Retrieved {} staff users", users.size());

            // Get departments (no token needed - handled by Feign client)
            List<DepartmentDTO> departments = userInfoClient.getDepartments();
            log.debug("Retrieved {} departments", departments.size());

            // Get leave type count
            long leaveTypeCount = leaveTypeRepository.count();
            log.debug("Retrieved {} leave types", leaveTypeCount);

            // Get active leave requests count
            long activeLeaveRequests = leaveRepository.countByStatus(Leave.LeaveStatus.PENDING);
            log.debug("Retrieved {} active leave requests", activeLeaveRequests);

            // Build response
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", users.size());
            stats.put("departmentCount", departments.size());
            stats.put("leaveTypeCount", leaveTypeCount);
            stats.put("activeLeaveRequests", activeLeaveRequests);

            log.info(
                    "Dashboard statistics retrieved successfully: {} users, {} departments, {} leave types, {} pending requests",
                    users.size(), departments.size(), leaveTypeCount, activeLeaveRequests);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Failed to fetch dashboard statistics: {}", e.getMessage(), e);
            throw new DashboardStatsException("Failed to retrieve dashboard statistics", e);
        }
    }
}