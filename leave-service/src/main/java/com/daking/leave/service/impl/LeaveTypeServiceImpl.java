package com.daking.leave.service.impl;

import com.daking.leave.dto.request.LeaveTypeRequest;
import com.daking.leave.dto.response.LeaveTypeResponse;
import com.daking.leave.model.LeaveType;
import com.daking.leave.repository.LeaveTypeRepository;
import com.daking.leave.service.interfaces.LeaveTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeaveTypeServiceImpl implements LeaveTypeService {
    private final LeaveTypeRepository leaveTypeRepository;

    @Autowired
    public LeaveTypeServiceImpl(LeaveTypeRepository leaveTypeRepository) {
        this.leaveTypeRepository = leaveTypeRepository;
    }

    @Override
    @Transactional
    public LeaveTypeResponse createLeaveType(LeaveTypeRequest request) {
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
        leaveTypeRepository.deleteById(id);
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