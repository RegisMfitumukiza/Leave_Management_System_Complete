package com.daking.auth.api.service;

import com.daking.auth.api.dto.DepartmentDTO;
import com.daking.auth.api.dto.UserResponseDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import java.util.List;

public interface UserInfoApi {

    @GetMapping("/api/auth/users/{userId}")
    UserResponseDTO getUserById(@PathVariable("userId") Long userId);

    @GetMapping("/api/auth/users/email/{email}")
    UserResponseDTO getUserByEmail(@PathVariable("email") String email);

    @GetMapping("/api/auth/users/{userId}/role")
    String getUserRole(@PathVariable("userId") Long userId);

    @GetMapping("/api/auth/users/team/{departmentId}")
    List<UserResponseDTO> getTeamMembers(@PathVariable("departmentId") Long departmentId);

    @GetMapping("/api/auth/users/managers/{departmentId}")
    List<UserResponseDTO> getManagers(@PathVariable("departmentId") Long departmentId);

    @GetMapping("/api/auth/profile")
    UserResponseDTO getUserProfile();

    @GetMapping("/api/auth/users/{managerId}/departments-managed")
    List<Long> getDepartmentsManaged(@PathVariable("managerId") Long managerId);

    @GetMapping("/api/auth/users/role/{role}")
    List<UserResponseDTO> getUsersByRole(@PathVariable("role") String role);

    @GetMapping("/api/auth/departments")
    List<DepartmentDTO> getDepartments();

    @PostMapping("/api/auth/users/by-ids")
    List<UserResponseDTO> getUsersByIds(@RequestBody List<Long> userIds);
}