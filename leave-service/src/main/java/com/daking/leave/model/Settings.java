package com.daking.leave.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

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

    // Comma-separated leave type names that require documents
    private String documentRequiredFor;

    // Optional: "single", "multi-level"
    private String approvalWorkflow;

    // Comma-separated: "email,in-app"
    private String notificationPreferences;

    // Optional: "internal", "external"
    private String holidayCalendarSource;
}