package com.daking.auth.service;

import com.daking.auth.model.Department;
import com.daking.auth.model.User;
import com.daking.auth.dto.DepartmentDTO;
import com.daking.auth.repository.DepartmentRepository;
import com.daking.auth.repository.UserRepository;
import com.daking.auth.exception.DepartmentNotFoundException;
import com.daking.auth.exception.DuplicateDepartmentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public Department createDepartment(Department department) {
        if (departmentRepository.existsByName(department.getName())) {
            throw new DuplicateDepartmentException("Department with name " + department.getName() + " already exists");
        }
        return departmentRepository.save(department);
    }

    @Override
    @Transactional
    public Department updateDepartment(Long id, Department department) {
        Department existingDepartment = departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found with id: " + id));

        if (!existingDepartment.getName().equals(department.getName())
                && departmentRepository.existsByName(department.getName())) {
            throw new DuplicateDepartmentException("Department with name " + department.getName() + " already exists");
        }

        existingDepartment.setName(department.getName());
        existingDepartment.setDescription(department.getDescription());
        existingDepartment.setIsActive(department.getIsActive());
        if (department.getManager() != null) {
            existingDepartment.setManager(department.getManager());
        } else {
            existingDepartment.setManager(null);
        }

        return departmentRepository.save(existingDepartment);
    }

    @Override
    @Transactional
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found with id: " + id));
        department.setIsActive(false);
        departmentRepository.save(department);
    }

    @Override
    public Optional<Department> getDepartment(Long id) {
        return departmentRepository.findById(id);
    }

    @Override
    public Optional<Department> getDepartmentByName(String name) {
        return departmentRepository.findByName(name);
    }

    @Override
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    @Override
    public List<Department> getActiveDepartments() {
        return departmentRepository.findByIsActiveTrue();
    }

    @Override
    public List<Department> getDepartmentsByManager(Long managerId) {
        return departmentRepository.findByManagerIdWithUsers(managerId);
    }

    @Override
    @Transactional
    public Department updateDepartmentManager(Long departmentId, Long managerId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found with id: " + departmentId));

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Manager not found with id: " + managerId));

        department.setManager(manager);
        return departmentRepository.save(department);
    }

    @Override
    @Transactional
    public Department updateDepartmentStatus(Long departmentId, boolean isActive) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found with id: " + departmentId));
        department.setIsActive(isActive);
        return departmentRepository.save(department);
    }

    @Override
    public List<Department> getDepartmentsCreatedBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return departmentRepository.findByCreatedAtBetween(startDate, endDate);
    }

    @Override
    public DepartmentDTO convertToDTO(Department department) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(department.getId());
        dto.setName(department.getName());
        dto.setDescription(department.getDescription());
        dto.setActive(department.getIsActive());
        if (department.getManager() != null) {
            dto.setManagerId(department.getManager().getId());
            dto.setManagerName(department.getManager().getFullName());
        } else {
            dto.setManagerId(null);
            dto.setManagerName(null);
        }
        dto.setCreatedAt(department.getCreatedAt());
        dto.setUpdatedAt(department.getUpdatedAt());
        // Always fetch users from repository to ensure users list is populated
        List<User> users = department.getUsers();
        if (users == null || users.isEmpty()) {
            users = userRepository.findByDepartmentId(department.getId());
        }
        if (users != null) {
            dto.setUsers(users.stream()
                    .map(com.daking.auth.dto.UserSummaryDTO::fromUser)
                    .collect(java.util.stream.Collectors.toList()));
        } else {
            dto.setUsers(java.util.Collections.emptyList());
        }
        return dto;
    }

    @Override
    public boolean existsByName(String name) {
        return departmentRepository.existsByName(name);
    }

    @Override
    public long countDepartments() {
        return departmentRepository.count();
    }
}