package com.daking.leave.controller;

import com.daking.leave.dto.request.LeaveTypeRequest;
import com.daking.leave.dto.response.LeaveTypeResponse;
import com.daking.leave.service.interfaces.LeaveTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.List;
import com.daking.leave.model.LeaveType;
import com.daking.leave.repository.LeaveTypeRepository;

@RestController
@RequestMapping("/api/leave-types")
@RequiredArgsConstructor
public class LeaveTypeController {
    private final LeaveTypeService leaveTypeService;
    private final LeaveTypeRepository leaveTypeRepository;

    // Create leave type
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LeaveTypeResponse> createLeaveType(@Valid @RequestBody LeaveTypeRequest request) {
        return ResponseEntity.ok(leaveTypeService.createLeaveType(request));
    }

    // Update leave type
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LeaveTypeResponse> updateLeaveType(@PathVariable Long id,
            @Valid @RequestBody LeaveTypeRequest request) {
        return ResponseEntity.ok(leaveTypeService.updateLeaveType(id, request));
    }

    // Delete leave type
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteLeaveType(@PathVariable Long id) {
        leaveTypeService.deleteLeaveType(id);
        return ResponseEntity.noContent().build();
    }

    // Get all leave types
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<LeaveType> getAllLeaveTypes() {
        return leaveTypeRepository.findAll();
    }

    // Get active leave types
    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LeaveTypeResponse>> getActiveLeaveTypes() {
        return ResponseEntity.ok(leaveTypeService.getActiveLeaveTypes());
    }

    // Get leave type by ID
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaveTypeResponse> getLeaveTypeById(@PathVariable Long id) {
        return ResponseEntity.ok(leaveTypeService.getLeaveTypeById(id));
    }
}