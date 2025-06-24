package com.daking.leave.model;

public enum NotificationType {
    LEAVE_APPLICATION, // For new leave submissions
    LEAVE_STATUS, // For updates on leave (approved, rejected)
    ANNOUNCEMENT, // For general announcements
    SYSTEM_ALERT // For system-level alerts
}