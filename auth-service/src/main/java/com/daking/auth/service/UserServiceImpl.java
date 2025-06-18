package com.daking.auth.service;

import com.daking.auth.model.User;
import com.daking.auth.model.userPrinciple;
import com.daking.auth.model.Role;
import com.daking.auth.repository.UserRepository;
import com.daking.auth.dto.LoginRequest;
import com.daking.auth.dto.UserResponseDTO;
import com.daking.auth.exception.*;
import com.daking.auth.validation.EmailRoleValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.daking.auth.repository.DepartmentRepository;
import com.daking.auth.model.Department;
import com.daking.auth.dto.DepartmentDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserRepository userRepository;
    private final JWTService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailRoleValidator emailRoleValidator;
    private final DepartmentRepository departmentRepository;

    @Override
    public String generateTokenForOAuthUser(String email) {
        String normalizedEmail = email == null ? null : email.toLowerCase().trim();
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
        User user;
        if (userOpt.isPresent()) {
            user = userOpt.get();
        } else {
            // JIT provision: create user with default STAFF role and no password
            user = new User();
            user.setEmail(normalizedEmail);
            user.setFirstName("Google"); // You may want to extract from OAuth2 principal if available
            user.setLastName("User");
            user.setRole(Role.STAFF);
            user.setActive(true);
            user.setGoogleId(null); // Set if available
            user.setLocale(null);
            user.setCreatedAt(java.time.LocalDateTime.now());
            user.setUpdatedAt(java.time.LocalDateTime.now());
            user.setUsername(normalizedEmail);
            user.setPassword("oauth2");

            // Assign to first available department ONLY if STAFF
            if (user.getRole() == Role.STAFF) {
                List<Department> activeDepartments = departmentRepository.findByIsActiveTrue();
                if (!activeDepartments.isEmpty()) {
                    user.setDepartmentId(activeDepartments.get(0).getId());
                }
            }

            userRepository.save(user);
        }
        return jwtService.generateToken(user);
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email == null ? null : email.toLowerCase().trim());
    }

    @Override
    @Transactional
    public User updateAvatar(Long userId, String avatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        user.setAvatarUrl(avatarUrl);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User register(User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new DuplicateUserException("User with email " + user.getEmail() + " already exists");
        }
        if (!emailRoleValidator.validateEmailRole(user)) {
            throw new ValidationException("Email domain does not match user role");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    public String verify(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new AuthenticationException("Invalid email or password");
        }
        if (!user.isActive()) {
            throw new AuthenticationException("User account is inactive");
        }
        UserDetails userDetails = new userPrinciple(user);
        return jwtService.generateToken(userDetails);
    }

    @Override
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
    }

    @Override
    public List<User> getUsersByDepartment(Long departmentId) {
        return userRepository.findByDepartmentId(departmentId);
    }

    @Override
    public List<User> getManagersByDepartment(Long departmentId) {
        return userRepository.findByDepartmentIdAndRole(departmentId, Role.MANAGER);
    }

    @Override
    public UserResponseDTO convertToDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setUsername(user.getUsername());
        dto.setRole(user.getRole());
        dto.setDepartmentId(user.getDepartmentId());
        dto.setIsActive(user.isActive());
        dto.setIsOnLeave(user.isOnLeave());
        dto.setHasPendingApprovals(user.hasPendingApprovals());
        dto.setManagerId(user.getManagerId());
        dto.setAvatarUrl(user.getAvatarUrl());
        if (user.getDepartmentId() != null) {
            Department department = departmentRepository.findById(user.getDepartmentId()).orElse(null);
            if (department != null) {
                DepartmentDTO deptDto = new DepartmentDTO();
                deptDto.setId(department.getId());
                deptDto.setName(department.getName());
                deptDto.setDescription(department.getDescription());
                deptDto.setActive(department.getIsActive());
                dto.setDepartment(deptDto);
            }
        }
        return dto;
    }

    @Override
    @Transactional
    public User registerWithGoogle(String email, String googleId, String firstName, String lastName, String locale) {
        return registerWithGoogle(email, googleId, firstName, lastName, locale, null);
    }

    @Override
    @Transactional
    public User registerWithGoogle(String email, String googleId, String firstName, String lastName, String locale,
            String pictureUrl) {
        email = email == null ? null : email.toLowerCase().trim();
        log.debug(
                "Attempting to register Google user: email={}, googleId={}, firstName={}, lastName={}, locale={}, pictureUrl={}",
                email, googleId, firstName, lastName, locale, pictureUrl);
        if (userRepository.findByEmail(email).isPresent()) {
            log.warn("Duplicate user by email: {}", email);
            throw new DuplicateUserException("User with email " + email + " already exists");
        }
        if (userRepository.findByGoogleId(googleId).isPresent()) {
            log.warn("Duplicate user by Google ID: {}", googleId);
            throw new DuplicateUserException("User with Google ID " + googleId + " already exists");
        }
        try {
        User user = new User();
        user.setEmail(email);
        user.setGoogleId(googleId);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(email);
            user.setPassword(UUID.randomUUID().toString()); // For Google users, set a random password
        user.setRole(Role.STAFF);
        user.setActive(true);
        user.setLastLogin(LocalDateTime.now());
            user.setLocale(locale);
            user.setCreatedAt(java.time.LocalDateTime.now());
            user.setUpdatedAt(java.time.LocalDateTime.now());

            // Set Google profile picture if available
            if (pictureUrl != null && !pictureUrl.trim().isEmpty()) {
                user.setAvatarUrl(pictureUrl);
                log.info("Set Google profile picture for user {}: {}", email, pictureUrl);
            }

            // Assign to first available department ONLY if STAFF
            if (user.getRole() == Role.STAFF) {
                List<Department> activeDepartments = departmentRepository.findByIsActiveTrue();
                if (!activeDepartments.isEmpty()) {
                    user.setDepartmentId(activeDepartments.get(0).getId());
                    log.info("Assigned user {} to department: {}", email, activeDepartments.get(0).getName());
                } else {
                    log.warn("No active departments found. User {} will not be assigned to any department.", email);
                }
            }

            User savedUser = userRepository.save(user);
            log.info("Google user created successfully: {} (ID: {})", savedUser.getEmail(), savedUser.getId());
            return savedUser;
        } catch (Exception e) {
            log.error("Error creating Google user: {}", email, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public User updateLastLogin(Long userId) {
        User user = getUserById(userId);
        user.setLastLogin(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    public List<User> getInactiveUsers(LocalDateTime beforeDate) {
        return userRepository.findInactiveUsers(beforeDate);
    }

    @Override
    public List<User> searchUsers(String query) {
        return userRepository.searchUsers(query.toLowerCase());
    }

    @Override
    @Transactional
    public User updateUserStatus(Long userId, boolean isActive) {
        User user = getUserById(userId);
        user.setActive(isActive);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateUserRole(Long userId, String role) {
        User user = getUserById(userId);
        try {
            Role userRole = Role.valueOf(role.toUpperCase());
            if (!emailRoleValidator.validateEmailRole(user)) {
                throw new ValidationException("Email domain does not match the new role");
            }
            user.setRole(userRole);
            return userRepository.save(user);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid role: " + role);
        }
    }

    @Override
    @Transactional
    public User updateUserDepartment(Long userId, Long departmentId) {
        User user = getUserById(userId);
        user.setDepartmentId(departmentId);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateUserManager(Long userId, Long managerId) {
        User user = getUserById(userId);
        user.setManager(managerId);
        return userRepository.save(user);
    }

    @Override
    public List<User> getUsersByRole(String role) {
        try {
            Role userRole = Role.valueOf(role.toUpperCase());
            return userRepository.findAll().stream()
                    .filter(user -> user.getRole() == userRole)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid role: " + role);
        }
    }

    @Override
    public List<User> getActiveUsers() {
        return userRepository.findByActiveTrue();
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        return new userPrinciple(user);
    }

    @Override
    public void bulkDeactivateUsers(List<Long> userIds) {
        for (Long id : userIds) {
            userRepository.findById(id).ifPresent(user -> {
                user.setIsActive(false);
                userRepository.save(user);
            });
        }
    }

    @Override
    @Transactional
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    @Override
    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new AuthenticationException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * Fix users without department assignment by assigning them to the first
     * available department
     * This method should be called during application startup or as a maintenance
     * task
     */
    @Transactional
    public int fixUsersWithoutDepartment() {
        List<Department> activeDepartments = departmentRepository.findByIsActiveTrue();
        if (activeDepartments.isEmpty()) {
            log.warn("No active departments found. Cannot fix users without department assignment.");
            return 0;
        }

        Long defaultDepartmentId = activeDepartments.get(0).getId();
        List<User> usersWithoutDepartment = userRepository.findAll().stream()
                .filter(user -> user.getDepartmentId() == null && user.isActive())
                .collect(Collectors.toList());

        int fixedCount = 0;
        for (User user : usersWithoutDepartment) {
            // Only assign department if STAFF
            if (user.getRole() == Role.STAFF) {
                user.setDepartmentId(defaultDepartmentId);
                userRepository.save(user);
                fixedCount++;
                log.info("Fixed user {} (ID: {}) by assigning to department: {}",
                        user.getEmail(), user.getId(), activeDepartments.get(0).getName());
            }
        }

        if (fixedCount > 0) {
            log.info("Fixed {} users without department assignment", fixedCount);
        }

        return fixedCount;
    }

    /**
     * Sync Google profile pictures for users who don't have avatars
     * This method should be called during application startup or as a maintenance
     * task
     */
    @Transactional
    public int syncGoogleProfilePictures() {
        List<User> usersWithoutAvatars = userRepository.findAll().stream()
                .filter(user -> user.getGoogleId() != null
                        && (user.getAvatarUrl() == null || user.getAvatarUrl().trim().isEmpty()))
                .collect(Collectors.toList());

        int syncedCount = 0;
        for (User user : usersWithoutAvatars) {
            try {
                // For existing users, we can't fetch their Google profile picture without their
                // consent
                // This would require additional OAuth scopes and user interaction
                // For now, we'll log that these users need manual avatar update
                log.info("User {} (ID: {}) has Google ID but no avatar. Manual update required.",
                        user.getEmail(), user.getId());
                syncedCount++;
            } catch (Exception e) {
                log.error("Error syncing profile picture for user {}: {}", user.getEmail(), e.getMessage());
            }
        }

        if (syncedCount > 0) {
            log.info("Found {} users with Google IDs but no avatars", syncedCount);
        }

        return syncedCount;
    }

    @Override
    public JWTService getJwtService() {
        return jwtService;
    }
}