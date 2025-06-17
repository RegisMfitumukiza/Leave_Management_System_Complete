package com.daking.auth.service;

import com.daking.auth.model.Department;
import com.daking.auth.dto.DepartmentDTO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DepartmentService {
    Department createDepartment(Department department);

    Department updateDepartment(Long id, Department department);

    void deleteDepartment(Long id);

    Optional<Department> getDepartment(Long id);

    Optional<Department> getDepartmentByName(String name);

    List<Department> getAllDepartments();

    List<Department> getActiveDepartments();

    List<Department> getDepartmentsByManager(Long managerId);

    Department updateDepartmentManager(Long departmentId, Long managerId);

    Department updateDepartmentStatus(Long departmentId, boolean isActive);

    List<Department> getDepartmentsCreatedBetween(LocalDateTime startDate, LocalDateTime endDate);

    DepartmentDTO convertToDTO(Department department);

    boolean existsByName(String name);

    long countDepartments();
}