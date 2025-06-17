package com.daking.leave.service.interfaces;

import com.daking.leave.dto.response.NotificationResponse;
import java.util.List;

public interface InAppNotificationService {
    void sendNotification(Long userId, String message, String type, Long relatedId, String link);

    List<NotificationResponse> getNotifications(Long userId);

    void markAsRead(Long notificationId);
}