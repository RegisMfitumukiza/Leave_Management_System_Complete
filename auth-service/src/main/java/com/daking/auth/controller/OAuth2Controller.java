package com.daking.auth.controller;

import com.daking.auth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/api/oauth2")
public class OAuth2Controller {

    @Autowired
    private UserService userService;

    @Autowired
    private OAuth2AuthorizedClientService clientService;

    @GetMapping("/authorization/google")
    public RedirectView initiateGoogleLogin() {
        // Add state parameter for CSRF protection
        String state = UUID.randomUUID().toString();
        // Store state in session or cache for validation
        return new RedirectView("/oauth2/authorization/google?state=" + state);
    }

    @GetMapping("/authorization/google/callback")
    public ResponseEntity<?> handleGoogleCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state,
            OAuth2AuthenticationToken authentication) {
        try {
            log.info("Received OAuth2 callback with code");

            // Validate state parameter if present
            if (state != null) {
                // Validate state from session/cache
                // This is a placeholder - implement proper state validation
                log.info("Validating state parameter: {}", state);
            }

            // Get the authorized client
            OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
                    "google",
                    authentication.getName());

            if (client == null) {
                throw new RuntimeException("Failed to get authorized client");
            }

            // Get the OAuth2User from the authentication
            OAuth2User oauth2User = authentication.getPrincipal();

            // Extract user information
            String email = oauth2User.getAttribute("email");
            String pictureUrl = oauth2User.getAttribute("picture");
            String googleId = oauth2User.getAttribute("sub");
            String firstName = oauth2User.getAttribute("given_name");
            String lastName = oauth2User.getAttribute("family_name");
            String locale = oauth2User.getAttribute("locale");
            boolean emailVerified = oauth2User.getAttribute("email_verified");

            log.info("Processing OAuth2 login for user: {}", email);

            // Validate email verification
            if (!emailVerified) {
                throw new RuntimeException("Email not verified with Google");
            }

            // Get or create user (Just-In-Time Provisioning)
            var userOpt = userService.getUserByEmail(email);
            log.debug("Result of getUserByEmail({}): {}", email, userOpt.isPresent());
            var user = userOpt
                    .map(existingUser -> {
                        log.info("Updating existing user: {}", email);
                        // Update user information if needed
                        if (!Objects.equals(existingUser.getGoogleId(), googleId)) {
                            existingUser.setGoogleId(googleId);
                        }
                        if (pictureUrl != null && !Objects.equals(existingUser.getAvatarUrl(), pictureUrl)) {
                            existingUser.setAvatarUrl(pictureUrl);
                        }
                        if (locale != null) {
                            existingUser.setLocale(locale);
                        }
                        return userService.updateUser(existingUser);
                    })
                    .orElseGet(() -> {
                        log.info("Creating new user for Google login: {}", email);
                        return userService.registerWithGoogle(email, googleId, firstName, lastName, locale, pictureUrl);
            });

            // Update last login
            user = userService.updateLastLogin(user.getId());

            // Generate JWT token with appropriate claims
            String token = userService.generateTokenForOAuthUser(email);

            log.info("Successfully authenticated user: {}", email);

            return ResponseEntity.ok()
                    .body(Map.of(
                            "token", token,
                            "user", userService.convertToDTO(user),
                            "isNewUser", user.getCreatedAt().equals(user.getUpdatedAt())));
        } catch (Exception e) {
            log.error("Error during OAuth callback", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Authentication failed: " + e.getMessage()));
        }
    }

    @GetMapping("/login-failure")
    public ResponseEntity<?> handleGoogleLoginFailure() {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Google login failed"));
    }
}