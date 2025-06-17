package com.daking.leave.controller;

import com.daking.leave.dto.request.LeaveBalanceAdjustmentRequest;
import com.daking.leave.dto.request.BulkAdjustRequest;
import com.daking.leave.dto.response.LeaveBalanceResponse;
import com.daking.leave.service.interfaces.LeaveBalanceService;
// import com.daking.leave.client.UserInfoClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/leave-balances")
@RequiredArgsConstructor
public class LeaveBalanceController {
    private final LeaveBalanceService leaveBalanceService;
    // private final UserInfoClient userInfoClient;

    // Get leave balances for user
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN') or #userId == principal.username")
    public ResponseEntity<List<LeaveBalanceResponse>> getLeaveBalancesByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(leaveBalanceService.getLeaveBalancesByUser(userId));
    }

    // Get my leave balances
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LeaveBalanceResponse>> getMyLeaveBalances(
            @AuthenticationPrincipal String userIdStr) {
        // userIdStr is userId as string (see JwtAuthenticationFilter)
        if (userIdStr == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(leaveBalanceService.getLeaveBalancesByUser(userId));
    }

    // Adjust leave balance (admin only)
    @PostMapping("/adjust")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<LeaveBalanceResponse> adjustLeaveBalance(
            @Valid @RequestBody LeaveBalanceAdjustmentRequest request) {
        return ResponseEntity.ok(leaveBalanceService.adjustLeaveBalance(request));
    }

    // Get leave balance for user/type/year
    @GetMapping("/user/{userId}/type/{leaveTypeId}/year/{year}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN') or #userId == principal.username")
    public ResponseEntity<LeaveBalanceResponse> getLeaveBalance(@PathVariable Long userId,
            @PathVariable Long leaveTypeId,
            @PathVariable Integer year) {
        return ResponseEntity.ok(leaveBalanceService.getLeaveBalance(userId, leaveTypeId, year));
    }

    @PostMapping("/bulk-adjust")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> bulkAdjustLeaveBalances(@RequestBody BulkAdjustRequest request) {
        leaveBalanceService.bulkAdjustLeaveBalances(request.getUserIds(), request.getLeaveTypeId(),
                request.getAdjustmentDays(), request.getReason());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/bulk")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<Map<Long, List<LeaveBalanceResponse>>> getBulkLeaveBalances(
            @RequestParam("userIds") String userIds) {
        List<Long> ids = java.util.Arrays.stream(userIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .toList();
        Map<Long, List<LeaveBalanceResponse>> result = new java.util.HashMap<>();
        for (Long id : ids) {
            result.put(id, leaveBalanceService.getLeaveBalancesByUser(id));
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/initialize")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> initializeMissingLeaveBalances() {
        int created = leaveBalanceService.initializeMissingLeaveBalances();
        return ResponseEntity.ok("Initialized " + created + " missing leave balances for the current year.");
    }
}