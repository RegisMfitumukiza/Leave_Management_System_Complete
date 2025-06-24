package com.daking.leave.service.impl;

import com.daking.leave.dto.request.LeaveTypeRequest;
import com.daking.leave.dto.response.LeaveTypeResponse;
import com.daking.leave.model.LeaveType;
import com.daking.leave.repository.LeaveTypeRepository;
import com.daking.leave.repository.LeaveRepository;
import com.daking.leave.repository.LeaveBalanceRepository;
import com.daking.leave.service.interfaces.LeaveTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class LeaveTypeServiceImpl implements LeaveTypeService {
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveRepository leaveRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;

    @Override
    @Transactional
    public LeaveTypeResponse createLeaveType(LeaveTypeRequest request) {
        log.info("Creating new leave type: {}", request.getName());

        // Validate request
        validateLeaveTypeRequest(request);

        LeaveType leaveType = new LeaveType();
        leaveType.setName(request.getName());
        leaveType.setDescription(request.getDescription());
        leaveType.setDefaultDays(request.getDefaultDays());
        leaveType.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        leaveType.setAccrualRate(request.getAccrualRate());
        leaveType.setCanCarryOver(request.getCanCarryOver());
        leaveType.setMaxCarryOverDays(request.getMaxCarryOverDays());
        leaveType.setRequiresApproval(request.getRequiresApproval());
        leaveType.setRequiresDocumentation(request.getRequiresDocumentation());
        leaveType.setIsPaid(request.getIsPaid());

        leaveType = leaveTypeRepository.save(leaveType);
        log.info("Successfully created leave type with ID: {}", leaveType.getId());
        return toResponse(leaveType);
    }

    @Override
    @Transactional
    public LeaveTypeResponse updateLeaveType(Long id, LeaveTypeRequest request) {
        LeaveType leaveType = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave type not found"));
        leaveType.setName(request.getName());
        leaveType.setDescription(request.getDescription());
        leaveType.setDefaultDays(request.getDefaultDays());
        leaveType.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        leaveType.setAccrualRate(request.getAccrualRate());
        leaveType.setCanCarryOver(request.getCanCarryOver());
        leaveType.setMaxCarryOverDays(request.getMaxCarryOverDays());
        leaveType.setRequiresApproval(request.getRequiresApproval());
        leaveType.setRequiresDocumentation(request.getRequiresDocumentation());
        leaveType.setIsPaid(request.getIsPaid());
        leaveType = leaveTypeRepository.save(leaveType);
        return toResponse(leaveType);
    }

    @Override
    public LeaveTypeResponse getLeaveTypeById(Long id) {
        LeaveType leaveType = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave type not found"));
        return toResponse(leaveType);
    }

    @Override
    public List<LeaveTypeResponse> getAllLeaveTypes() {
        return leaveTypeRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<LeaveTypeResponse> getActiveLeaveTypes() {
        return leaveTypeRepository.findAll().stream()
                .filter(LeaveType::getIsActive)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteLeaveType(Long id) {
        log.info("Attempting to delete leave type with ID: {}", id);

        LeaveType leaveType = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave type not found with ID: " + id));

        // Check if leave type is currently in use
        validateLeaveTypeDeletion(leaveType);

        leaveTypeRepository.deleteById(id);
        log.info("Successfully deleted leave type with ID: {}", id);
    }

    private void validateLeaveTypeRequest(LeaveTypeRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Leave type name cannot be null or empty");
        }

        if (request.getDefaultDays() == null || request.getDefaultDays() <= 0) {
            throw new IllegalArgumentException("Default days must be greater than 0");
        }

        if (request.getAccrualRate() != null && request.getAccrualRate() < 0) {
            throw new IllegalArgumentException("Accrual rate cannot be negative");
        }

        if (request.getMaxCarryOverDays() != null && request.getMaxCarryOverDays() < 0) {
            throw new IllegalArgumentException("Max carry over days cannot be negative");
        }

        // Check for duplicate names (for create operations)
        boolean nameExists = leaveTypeRepository.findAll().stream()
                .anyMatch(lt -> lt.getName().equals(request.getName()));
        if (nameExists) {
            throw new IllegalArgumentException("Leave type with name '" + request.getName() + "' already exists");
        }
    }

    private void validateLeaveTypeDeletion(LeaveType leaveType) {
        // Check if there are any active leaves using this leave type
        long activeLeavesCount = leaveRepository.findAll().stream()
                .filter(leave -> leave.getLeaveType().getId().equals(leaveType.getId()))
                .count();

        if (activeLeavesCount > 0) {
            throw new IllegalStateException("Cannot delete leave type '" + leaveType.getName() +
                    "' because it has " + activeLeavesCount + " associated leave records");
        }

        // Check if there are any leave balances using this leave type
        long leaveBalancesCount = leaveBalanceRepository.findAll().stream()
                .filter(balance -> balance.getLeaveType().getId().equals(leaveType.getId()))
                .count();

        if (leaveBalancesCount > 0) {
            throw new IllegalStateException("Cannot delete leave type '" + leaveType.getName() +
                    "' because it has " + leaveBalancesCount + " associated leave balance records");
        }

        log.info("Leave type '{}' is safe to delete - no associated records found", leaveType.getName());
    }

    private LeaveTypeResponse toResponse(LeaveType leaveType) {
        LeaveTypeResponse dto = new LeaveTypeResponse();
        dto.setId(leaveType.getId());
        dto.setName(leaveType.getName());
        dto.setDescription(leaveType.getDescription());
        dto.setDefaultDays(leaveType.getDefaultDays());
        dto.setIsActive(leaveType.getIsActive());
        dto.setAccrualRate(leaveType.getAccrualRate());
        dto.setCanCarryOver(leaveType.getCanCarryOver());
        dto.setMaxCarryOverDays(leaveType.getMaxCarryOverDays());
        dto.setRequiresApproval(leaveType.getRequiresApproval());
        dto.setRequiresDocumentation(leaveType.getRequiresDocumentation());
        dto.setIsPaid(leaveType.getIsPaid());
        return dto;
    }
}