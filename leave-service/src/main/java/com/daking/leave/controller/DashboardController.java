package com.daking.leave.controller;

import com.daking.leave.client.UserInfoClient;
import com.daking.leave.repository.LeaveTypeRepository;
import com.daking.leave.repository.LeaveRepository;
import com.daking.leave.security.ServiceAccountTokenProvider;
import com.daking.leave.model.Leave;
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
public class DashboardController {
    private final UserInfoClient userInfoClient;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveRepository leaveRepository;
    private final ServiceAccountTokenProvider serviceAccountTokenProvider;

    public DashboardController(UserInfoClient userInfoClient,
            LeaveTypeRepository leaveTypeRepository,
            LeaveRepository leaveRepository,
            ServiceAccountTokenProvider serviceAccountTokenProvider) {
        this.userInfoClient = userInfoClient;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveRepository = leaveRepository;
        this.serviceAccountTokenProvider = serviceAccountTokenProvider;
    }

    @GetMapping("/dashboard-stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        String token = serviceAccountTokenProvider.getToken();
        List<?> users = userInfoClient.getUsersByRole("STAFF", token); // Or get all users if needed
        List<?> departments = userInfoClient.getDepartments(token);
        long leaveTypeCount = leaveTypeRepository.count();
        long activeLeaveRequests = leaveRepository.countByStatus(Leave.LeaveStatus.PENDING);
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", users.size());
        stats.put("departmentCount", departments.size());
        stats.put("leaveTypeCount", leaveTypeCount);
        stats.put("activeLeaveRequests", activeLeaveRequests);
        return ResponseEntity.ok(stats);
    }
}