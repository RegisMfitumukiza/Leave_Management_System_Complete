package com.daking.leave.controller;

import com.daking.auth.api.dto.UserResponseDTO;
import com.daking.leave.client.UserInfoClient;
import com.daking.leave.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserInfoClient userInfoClient;

    /**
     * Get the current user's profile
     * 
     * @return UserResponseDTO containing the current user's profile information
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDTO> getProfile(@AuthenticationPrincipal String userIdStr) {
        log.debug("Fetching profile for user: {}", userIdStr);

        if (userIdStr == null) {
            log.warn("User ID is null in authentication principal");
            return ResponseEntity.status(401).build();
        }

        try {
            Long userId = Long.parseLong(userIdStr);

            // Get user profile (no token needed - handled by Feign client)
            UserResponseDTO user = userInfoClient.getUserById(userId);

            if (user == null) {
                log.warn("User profile not found for user ID: {}", userId);
                throw new UserNotFoundException("User profile not found for ID: " + userId);
            }

            log.info("User profile retrieved successfully for user ID: {}", userId);
            return ResponseEntity.ok(user);

        } catch (NumberFormatException e) {
            log.error("Invalid user ID format: {}", userIdStr, e);
            return ResponseEntity.status(401).build();
        } catch (UserNotFoundException e) {
            log.warn("User not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to fetch user profile for user {}: {}", userIdStr, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
}