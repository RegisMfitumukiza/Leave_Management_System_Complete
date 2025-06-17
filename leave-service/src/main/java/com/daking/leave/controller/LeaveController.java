package com.daking.leave.controller;

import com.daking.leave.dto.request.LeaveApplicationRequest;
import com.daking.leave.dto.request.LeaveApprovalRequest;
import com.daking.leave.dto.response.LeaveResponse;
import com.daking.leave.service.interfaces.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveController {
    private final LeaveService leaveService;

    // Apply for leave
    @PostMapping
    @PreAuthorize("hasAnyRole('STAFF','EMPLOYEE')")
    public ResponseEntity<LeaveResponse> applyForLeave(@AuthenticationPrincipal String userIdStr,
            @Valid @RequestBody LeaveApplicationRequest request) {
        if (userIdStr == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(leaveService.applyForLeave(userIdStr, request));
    }

    // Approve leave
    @PostMapping("/{leaveId}/approve")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<LeaveResponse> approveLeave(@PathVariable Long leaveId,
            @AuthenticationPrincipal String approverEmail,
            @Valid @RequestBody LeaveApprovalRequest request) {
        if (approverEmail == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(leaveService.approveLeave(leaveId, approverEmail, request));
    }

    // Reject leave
    @PostMapping("/{leaveId}/reject")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<LeaveResponse> rejectLeave(@PathVariable Long leaveId,
            @AuthenticationPrincipal String approverEmail,
            @Valid @RequestBody LeaveApprovalRequest request) {
        if (approverEmail == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(leaveService.rejectLeave(leaveId, approverEmail, request));
    }

    // Cancel leave
    @PostMapping("/{leaveId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaveResponse> cancelLeave(@PathVariable Long leaveId,
            @AuthenticationPrincipal String userEmail) {
        if (userEmail == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(leaveService.cancelLeave(leaveId, userEmail));
    }

    // Get leave by ID
    @GetMapping("/{leaveId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaveResponse> getLeaveById(@PathVariable Long leaveId) {
        return ResponseEntity.ok(leaveService.getLeaveById(leaveId));
    }

    // Get leaves by user
    @GetMapping("/user/email/{userEmail}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN') or #userEmail == principal.username")
    public ResponseEntity<List<LeaveResponse>> getLeavesByUser(@PathVariable String userEmail) {
        return ResponseEntity.ok(leaveService.getLeavesByUser(userEmail));
    }

    // Get my leaves
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LeaveResponse>> getMyLeaves(@AuthenticationPrincipal String userIdStr) {
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
        return ResponseEntity.ok(leaveService.getLeavesByUserId(userId));
    }

    // Get pending leaves for manager
    @GetMapping("/pending")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<List<LeaveResponse>> getPendingLeaves(@AuthenticationPrincipal String userEmail) {
        if (userEmail == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(leaveService.getPendingLeaves(userEmail));
    }

    // Get team calendar by department
    @GetMapping("/team-calendar/{departmentId}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<List<LeaveResponse>> getTeamCalendar(@PathVariable Long departmentId,
            @RequestParam(required = false) String month) {
        return ResponseEntity.ok(leaveService.getTeamCalendar(departmentId, month));
    }

    // Get unified team calendar for manager
    @GetMapping("/team-calendar")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<List<LeaveResponse>> getTeamCalendarForManager(
            @AuthenticationPrincipal String userEmail,
            @RequestParam(required = false) String month) {
        if (userEmail == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(leaveService.getTeamCalendarForManager(userEmail, month));
    }

    // Search leaves
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<List<LeaveResponse>> searchLeaves(@RequestParam String query) {
        return ResponseEntity.ok(leaveService.searchLeaves(query));
    }

    @GetMapping("/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LeaveResponse>> getRecentLeaves() {
        return ResponseEntity.ok(leaveService.getRecentLeaves());
    }

    @GetMapping("/system-stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLeaves", leaveService.countAllLeaves());
        stats.put("pendingLeaves", leaveService.countLeavesByStatus("PENDING"));
        stats.put("approvedLeaves", leaveService.countLeavesByStatus("APPROVED"));
        stats.put("rejectedLeaves", leaveService.countLeavesByStatus("REJECTED"));
        return ResponseEntity.ok(stats);
    }

    @GetMapping
    public ResponseEntity<List<LeaveResponse>> getLeaves(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) List<Long> userIds) {
        if (userIds != null && !userIds.isEmpty()) {
            return ResponseEntity.ok(leaveService.getLeavesByUserIds(userIds));
        } else if (userId != null) {
            return ResponseEntity.ok(leaveService.getLeavesByUserId(userId));
        } else {
            return ResponseEntity.ok(leaveService.getAllLeaves());
        }
    }

    // Get team calendar for staff (their department only)
    @GetMapping("/team-calendar/staff")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<List<LeaveResponse>> getStaffTeamCalendar(
            @AuthenticationPrincipal String userIdStr,
            @RequestParam(required = false) String month) {
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
        return ResponseEntity.ok(leaveService.getStaffTeamCalendar(userId, month));
    }
}