package com.daking.leave.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type; // employee, department, leaveType, team-leave, approval, coverage

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false)
    private String generatedBy;

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    @Column(nullable = false)
    private String fileType; // excel, csv

    @Column(nullable = false)
    private String filePath;

    // Optional fields for manager reports
    @Column
    private Long managerId; // For manager-specific reports
}