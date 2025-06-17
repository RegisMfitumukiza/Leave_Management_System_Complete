package com.daking.auth.service;

import com.daking.auth.model.User;
import com.daking.auth.dto.UserResponseDTO;
import com.daking.auth.dto.LoginRequest;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;

public interface UserService {
    String generateTokenForOAuthUser(String email);

    Optional<User> getUserByEmail(String email);

    User updateAvatar(Long userId, String avatarUrl);

    User register(User user);

    String verify(LoginRequest loginRequest);

    User getUserById(Long userId);

    List<User> getUsersByDepartment(Long departmentId);

    List<User> getManagersByDepartment(Long departmentId);

    List<User> getUsersByRole(String role);

    List<User> getActiveUsers();

    List<User> getAllUsers();

    // NOTE: For authentication, always use loadUserByUsername(email) as username is
    // the email.
    UserDetails loadUserByUsername(String username);

    UserResponseDTO convertToDTO(User user);

    User registerWithGoogle(String email, String googleId, String firstName, String lastName, String locale);

    User registerWithGoogle(String email, String googleId, String firstName, String lastName, String locale,
            String pictureUrl);

    User updateLastLogin(Long userId);

    List<User> getInactiveUsers(LocalDateTime beforeDate);

    List<User> searchUsers(String query);

    User updateUserStatus(Long userId, boolean isActive);

    User updateUserRole(Long userId, String role);

    User updateUserDepartment(Long userId, Long departmentId);

    User updateUserManager(Long userId, Long managerId);

    void bulkDeactivateUsers(List<Long> userIds);

    User updateUser(User user);

    void deleteUser(Long userId);

    void changePassword(String email, String currentPassword, String newPassword);

    JWTService getJwtService();

    /**
     * Fix users without department assignment by assigning them to the first
     * available department
     * 
     * @return number of users fixed
     */
    int fixUsersWithoutDepartment();

    /**
     * Sync Google profile pictures for users who don't have avatars
     * 
     * @return number of users found without avatars
     */
    int syncGoogleProfilePictures();
}