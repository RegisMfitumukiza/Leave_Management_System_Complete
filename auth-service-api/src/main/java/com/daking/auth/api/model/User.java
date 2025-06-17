package com.daking.auth.api.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.URL;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @NotNull
    private Role role;

    private Long departmentId;

    private boolean active = true;

    private boolean onLeave = false;

    private boolean pendingApprovals = false;

    private Long managerId;

    private boolean emailNotificationsEnabled = true;

    @URL(message = "Avatar URL must be a valid URL")
    private String avatarUrl;

    private String googleId;

    private LocalDateTime lastLogin;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public String getFullName() {
        return firstName + " " + lastName;
    }

    @JsonProperty("isActive")
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @JsonProperty("isOnLeave")
    public boolean isOnLeave() {
        return onLeave;
    }

    public void setOnLeave(boolean onLeave) {
        this.onLeave = onLeave;
    }

    @JsonProperty("hasPendingApprovals")
    public boolean hasPendingApprovals() {
        return pendingApprovals;
    }

    public void setPendingApprovals(boolean pendingApprovals) {
        this.pendingApprovals = pendingApprovals;
    }

    @JsonProperty("emailNotificationsEnabled")
    public boolean isEmailNotificationsEnabled() {
        return emailNotificationsEnabled;
    }
}