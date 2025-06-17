package com.daking.leave.service.interfaces;

public interface NotificationService {
    void sendEmail(String to, String subject, String body);

    void sendInAppNotification(Long userId, String message);

    void sendInAppNotification(Long userId, String message, String type, Long relatedId, String link);
}