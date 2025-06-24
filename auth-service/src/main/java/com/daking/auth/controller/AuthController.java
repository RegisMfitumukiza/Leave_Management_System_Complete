package com.daking.auth.controller;

import com.daking.auth.dto.*;
import com.daking.auth.model.User;
import com.daking.auth.model.Role;
import com.daking.auth.dto.LoginRequest;
import com.daking.auth.service.UserService;
import com.daking.auth.service.JWTService;
import com.daking.auth.service.DepartmentService;
import com.daking.auth.exception.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.daking.auth.model.Department;
import com.daking.auth.repository.UserRepository;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private final UserService userService;
    private final JWTService jwtService;
    private final DepartmentService departmentService;
    private final UserRepository userRepository;

    @Operation(summary = "Register a new user", description = "Creates a new user account with STAFF role")
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> registerUser(@Valid @RequestBody RegistrationDto registrationDto) {
        User user = new User();
        user.setFirstName(registrationDto.getFirstName());
        user.setLastName(registrationDto.getLastName());
        user.setEmail(registrationDto.getEmail());
        user.setRole(Role.STAFF);
        user.setPassword(registrationDto.getPassword());

        User registeredUser = userService.register(user);
        String accessToken = jwtService.generateToken(registeredUser);
        String refreshToken = jwtService.generateRefreshToken(registeredUser);

        return ResponseEntity.ok(AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L) // 24 hours in seconds
                .user(userService.convertToDTO(registeredUser))
                .build());
    }

    @Operation(summary = "Login user", description = "Authenticates user and returns JWT tokens")
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        String token = userService.verify(loginRequest);
        User user = userService.getUserByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new AuthenticationException("User not found"));

        return ResponseEntity.ok(AuthResponseDTO.builder()
                .accessToken(token)
                .refreshToken(jwtService.generateRefreshToken(user))
                .tokenType("Bearer")
                .expiresIn(86400L)
                .user(userService.convertToDTO(user))
                .build());
    }

    @Operation(summary = "Refresh token", description = "Generates new access token using refresh token")
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponseDTO> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO request) {
        String email = jwtService.extractUsername(request.getRefreshToken());
        User user = userService.getUserByEmail(email)
                .orElseThrow(() -> new AuthenticationException("Invalid refresh token"));

        if (!jwtService.isTokenValid(request.getRefreshToken(), user)) {
            throw new AuthenticationException("Invalid refresh token");
        }

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return ResponseEntity.ok(AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .user(userService.convertToDTO(user))
                .build());
    }

    @Operation(summary = "Get user profile", description = "Returns the authenticated user's profile")
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDTO> getProfile(@AuthenticationPrincipal Object principal) {
        String email = extractEmailFromPrincipal(principal);
        if (email == null) {
            return ResponseEntity.status(401).build();
        }
        return userService.getUserByEmail(email)
                .map(userService::convertToDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Update user avatar", description = "Updates the authenticated user's avatar URL")
    @PostMapping("/avatar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDTO> updateAvatar(
            @AuthenticationPrincipal Object principal,
            @Valid @RequestBody Map<String, String> payload) {
        String email = extractEmailFromPrincipal(principal);
        if (email == null) {
            return ResponseEntity.status(401).build();
        }
        String avatarUrl = payload.get("avatarUrl");
        if (avatarUrl == null) {
            throw new ValidationException("Avatar URL is required");
        }
        return userService.getUserByEmail(email)
                .map(user -> ResponseEntity.ok(userService.convertToDTO(
                        userService.updateAvatar(user.getId(), avatarUrl))))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/users/{userId}/email")
    public ResponseEntity<String> getUserEmail(@PathVariable Long userId) {
        try {
            User user = userService.getUserById(userId);
            return ResponseEntity.ok(user.getEmail());
        } catch (UserNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // New endpoints to support leave management
    @GetMapping("/users/team/{departmentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserResponseDTO>> getTeamMembers(@PathVariable Long departmentId) {
        try {
            List<User> users = userService.getUsersByDepartment(departmentId);
            List<UserResponseDTO> dtos = users.stream()
                    .map(userService::convertToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (DepartmentNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/users/managers/{departmentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserResponseDTO>> getManagers(@PathVariable Long departmentId) {
        try {
            List<User> managers = userService.getManagersByDepartment(departmentId);
            List<UserResponseDTO> dtos = managers.stream()
                    .map(userService::convertToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (DepartmentNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/users/{userId}/role")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> getUserRole(@PathVariable Long userId) {
        try {
            User user = userService.getUserById(userId);
            return ResponseEntity.ok(user.getRole().name());
        } catch (UserNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<UserResponseDTO> dtos = users.stream()
                .map(userService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/users/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<List<UserResponseDTO>> searchUsers(@RequestParam String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new ValidationException("Query parameter must not be empty");
        }
        List<User> users = userService.searchUsers(query);
        List<UserResponseDTO> dtos = users.stream()
                .map(userService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/users/{managerId}/departments-managed")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Long>> getDepartmentsManaged(@PathVariable Long managerId) {
        List<com.daking.auth.model.Department> departments = departmentService.getDepartmentsByManager(managerId);
        List<Long> ids = departments.stream().map(com.daking.auth.model.Department::getId).toList();
        return ResponseEntity.ok(ids);
    }

    // Department endpoints (flattened from DepartmentsApi)
    @GetMapping("/departments")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<List<DepartmentDTO>> getAllDepartments() {
        List<Department> departments = departmentService.getAllDepartments();
        List<DepartmentDTO> dtos = departments.stream().map(departmentService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/departments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentDTO> createDepartment(@Valid @RequestBody DepartmentDTO dto) {
        Department department = new Department();
        department.setName(dto.getName());
        department.setDescription(dto.getDescription());
        department.setIsActive(dto.isActive());
        if (dto.getManagerId() != null) {
            User manager = userRepository.findById(dto.getManagerId())
                    .orElseThrow(() -> new UserNotFoundException("Manager not found"));
            department.setManager(manager);
        }
        Department created = departmentService.createDepartment(department);
        return ResponseEntity.ok(departmentService.convertToDTO(created));
    }

    @PutMapping("/departments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentDTO> updateDepartment(@PathVariable Long id,
            @Valid @RequestBody DepartmentDTO dto) {
        Department department = new Department();
        department.setName(dto.getName());
        department.setDescription(dto.getDescription());
        department.setIsActive(dto.isActive());
        if (dto.getManagerId() != null) {
            User manager = userRepository.findById(dto.getManagerId())
                    .orElseThrow(() -> new UserNotFoundException("Manager not found"));
            department.setManager(manager);
        }
        Department updated = departmentService.updateDepartment(id, department);
        return ResponseEntity.ok(departmentService.convertToDTO(updated));
    }

    @DeleteMapping("/departments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/departments/by-manager/{managerId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<List<DepartmentDTO>> getDepartmentsByManager(@PathVariable Long managerId) {
        List<Department> departments = departmentService.getDepartmentsByManager(managerId);
        List<DepartmentDTO> dtos = departments.stream().map(departmentService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/departments/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentDTO> activateDepartment(@PathVariable Long id) {
        Department updated = departmentService.updateDepartmentStatus(id, true);
        return ResponseEntity.ok(departmentService.convertToDTO(updated));
    }

    @PutMapping("/departments/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentDTO> deactivateDepartment(@PathVariable Long id) {
        Department updated = departmentService.updateDepartmentStatus(id, false);
        return ResponseEntity.ok(departmentService.convertToDTO(updated));
    }

    @PutMapping("/departments/assign-user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> assignUserToDepartment(@RequestParam Long userId, @RequestParam Long departmentId) {
        userService.updateUserDepartment(userId, departmentId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/departments/remove-user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeUserFromDepartment(@RequestParam Long userId) {
        userService.updateUserDepartment(userId, null);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users/email/{email}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('STAFF')")
    public ResponseEntity<UserResponseDTO> getUserByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email)
                .map(userService::convertToDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/users/role/{role}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserResponseDTO>> getUsersByRole(@PathVariable String role) {
        List<User> users = userService.getUsersByRole(role);
        List<UserResponseDTO> dtos = users.stream()
                .map(userService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/users/bulk-deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> bulkDeactivateUsers(@RequestBody List<Long> userIds) {
        userService.bulkDeactivateUsers(userIds);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        User user = userService.getUserById(id);
        if (payload.containsKey("firstName"))
            user.setFirstName(payload.get("firstName"));
        if (payload.containsKey("lastName"))
            user.setLastName(payload.get("lastName"));
        if (payload.containsKey("email"))
            user.setEmail(payload.get("email"));
        User updated = userService.updateUser(user);
        return ResponseEntity.ok(userService.convertToDTO(updated));
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        userService.updateUserStatus(id, false);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activateUser(@PathVariable Long id) {
        userService.updateUserStatus(id, true);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/{id}/hard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> hardDeleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/fix-departments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> fixUsersWithoutDepartment() {
        try {
            int fixedCount = userService.fixUsersWithoutDepartment();
            return ResponseEntity.ok(Map.of(
                    "message", "Successfully fixed users without department assignment",
                    "fixedCount", fixedCount));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to fix users without department assignment",
                    "message", e.getMessage()));
        }
    }

    @PostMapping("/users/sync-profile-pictures")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> syncGoogleProfilePictures() {
        try {
            int syncedCount = userService.syncGoogleProfilePictures();
            return ResponseEntity.ok(Map.of(
                    "message", "Successfully synced Google profile pictures",
                    "syncedCount", syncedCount));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to sync Google profile pictures",
                    "message", e.getMessage()));
        }
    }

    @Operation(summary = "Change user password", description = "Allows authenticated user to change their password")
    @PutMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal Object principal,
            @RequestBody Map<String, String> payload) {
        String email = extractEmailFromPrincipal(principal);
        if (email == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        String currentPassword = payload.get("currentPassword");
        String newPassword = payload.get("newPassword");
        if (currentPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body("Current and new password are required");
        }
        try {
            userService.changePassword(email, currentPassword, newPassword);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Update own profile", description = "Allows authenticated user to update their own profile info")
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDTO> updateOwnProfile(@AuthenticationPrincipal Object principal,
            @RequestBody Map<String, String> payload) {
        String email = extractEmailFromPrincipal(principal);
        if (email == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userService.getUserByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        if (payload.containsKey("firstName"))
            user.setFirstName(payload.get("firstName"));
        if (payload.containsKey("lastName"))
            user.setLastName(payload.get("lastName"));
        if (payload.containsKey("email"))
            user.setEmail(payload.get("email"));
        User updated = userService.updateUser(user);
        return ResponseEntity.ok(userService.convertToDTO(updated));
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('STAFF')")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(userService.convertToDTO(user));
    }

    @PostMapping("/users/by-ids")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserResponseDTO>> getUsersByIds(@RequestBody List<Long> userIds) {
        List<User> users = userRepository.findAllById(userIds);
        List<UserResponseDTO> dtos = users.stream()
                .map(userService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // Utility method to extract email from principal
    private String extractEmailFromPrincipal(Object principal) {
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            return ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
        } else if (principal instanceof org.springframework.security.oauth2.jwt.Jwt) {
            return ((org.springframework.security.oauth2.jwt.Jwt) principal).getSubject();
        } else if (principal instanceof org.springframework.security.oauth2.core.user.DefaultOAuth2User) {
            Object email = ((org.springframework.security.oauth2.core.user.DefaultOAuth2User) principal)
                    .getAttribute("email");
            return email != null ? email.toString() : null;
        } else if (principal instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) {
            org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken token = (org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) principal;
            Object email = token.getPrincipal().getAttribute("email");
            return email != null ? email.toString() : null;
        }
        return null;
    }

    // Exception handlers
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException e) {
        ErrorResponse response = new ErrorResponse();
        response.setError("Authentication failed");
        response.setMessage(e.getMessage());
        return ResponseEntity.status(401).body(response);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException e) {
        ErrorResponse response = new ErrorResponse();
        response.setError("Validation failed");
        response.setMessage(e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException e) {
        ErrorResponse response = new ErrorResponse();
        response.setError("User not found");
        response.setMessage(e.getMessage());
        return ResponseEntity.status(404).body(response);
    }

    @ExceptionHandler(DepartmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDepartmentNotFoundException(DepartmentNotFoundException e) {
        ErrorResponse response = new ErrorResponse();
        response.setError("Department not found");
        response.setMessage(e.getMessage());
        return ResponseEntity.status(404).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        e.printStackTrace(); // Log the exception to the console
        ErrorResponse response = new ErrorResponse();
        response.setError("Internal server error");
        response.setMessage(e.getMessage());
        return ResponseEntity.status(500).body(response);
    }
}