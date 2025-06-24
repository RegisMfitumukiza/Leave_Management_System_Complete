package com.daking.leave.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.Set;

@Entity
@Table(name = "settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double accrualRate; // e.g. 1.66
    private int maxCarryover; // e.g. 5
    private LocalDate carryoverExpiryDate; // e.g. 2024-01-31

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "settings_document_required_leavetypes", joinColumns = @JoinColumn(name = "settings_id"), inverseJoinColumns = @JoinColumn(name = "leavetype_id"))
    private Set<LeaveType> documentRequiredFor;

    @ElementCollection(targetClass = NotificationType.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "settings_notification_preferences", joinColumns = @JoinColumn(name = "settings_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type")
    private Set<NotificationType> notificationPreferences;

    // Optional: "single", "multi-level"
    private String approvalWorkflow;

    // Optional: "internal", "external"
    private String holidayCalendarSource;
}