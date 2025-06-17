package com.daking.leave.client;

import com.daking.auth.api.model.User;
import com.daking.auth.api.dto.UserResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "auth-service", configuration = UserInfoFeignConfig.class)
public interface UserInfoClient {
        @GetMapping("/api/auth/users/{userId}")
        User getUserById(@PathVariable("userId") Long userId, @RequestHeader("Authorization") String token);

        @GetMapping("/api/auth/profile")
        User getUserProfile(@RequestHeader("Authorization") String token);

        @GetMapping("/api/auth/users/team/{departmentId}")
        List<UserResponseDTO> getTeamMembers(@PathVariable("departmentId") Long departmentId,
                        @RequestHeader("Authorization") String token);

        @GetMapping("/api/auth/users/managers/{departmentId}")
        List<UserResponseDTO> getManagersByDepartment(@PathVariable("departmentId") Long departmentId,
                        @RequestHeader("Authorization") String token);

        @GetMapping("/api/auth/users/{userId}/role")
        String getUserRole(@PathVariable("userId") Long userId, @RequestHeader("Authorization") String token);

        @GetMapping("/api/auth/users/{managerId}/departments-managed")
        List<Long> getDepartmentsManaged(@PathVariable("managerId") Long managerId,
                        @RequestHeader(value = "Authorization", required = false) String token);

        @GetMapping("/api/auth/users/role/{role}")
        List<User> getUsersByRole(@PathVariable("role") String role, @RequestHeader("Authorization") String token);

        @GetMapping("/api/auth/users/email/{email}")
        UserResponseDTO getUserByEmail(@PathVariable("email") String email,
                        @RequestHeader(value = "Authorization", required = false) String token);

        @GetMapping("/api/auth/departments")
        List<Object> getDepartments(@RequestHeader("Authorization") String token);

        @GetMapping("/api/auth/users/{userId}/email")
        String getUserEmail(@PathVariable("userId") Long userId,
                        @RequestHeader(value = "Authorization", required = false) String token);
}