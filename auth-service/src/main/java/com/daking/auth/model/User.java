package com.daking.auth.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.URL;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @NotBlank
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @NotBlank
    @Email
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String username;

    @NotBlank
    @Column(nullable = false)
    private String password;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "department_id")
    private Long departmentId; // Reference to department in leave-service

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", insertable = false, updatable = false)
    private Department department;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "is_on_leave", nullable = false)
    private boolean onLeave = false;

    @Column(name = "has_pending_approvals", nullable = false)
    private boolean pendingApprovals = false;

    @Column(name = "manager_id")
    private Long managerId;

    @Column(name = "email_notifications_enabled", nullable = false)
    private boolean emailNotificationsEnabled = true;

    @Column(name = "avatar_url")
    @URL(message = "Avatar URL must be a valid URL")
    private String avatarUrl;

    @Column(name = "google_id", unique = true)
    private String googleId;

    @Column(name = "locale")
    private String locale;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private static final String DEFAULT_AVATAR_URL = "https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y";

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

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

    public void setEmailNotificationsEnabled(boolean emailNotificationsEnabled) {
        this.emailNotificationsEnabled = emailNotificationsEnabled;
    }

    public String getAvatarUrl() {
        return avatarUrl != null ? avatarUrl : DEFAULT_AVATAR_URL;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl != null ? avatarUrl : DEFAULT_AVATAR_URL;
    }

    public boolean isManager() {
        return role == Role.MANAGER;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isStaff() {
        return role == Role.STAFF;
    }

    public void setManager(Long managerId) {
        this.managerId = managerId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    public void setIsActive(boolean isActive) {
        this.active = isActive;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }
}
