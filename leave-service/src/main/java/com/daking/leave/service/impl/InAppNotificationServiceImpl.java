package com.daking.leave.service.impl;

import com.daking.leave.dto.response.NotificationResponse;
import com.daking.leave.exception.NotificationNotFoundException;
import com.daking.leave.exception.ValidationException;
import com.daking.leave.model.Notification;
import com.daking.leave.repository.NotificationRepository;
import com.daking.leave.service.interfaces.InAppNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InAppNotificationServiceImpl implements InAppNotificationService {
    private final NotificationRepository notificationRepository;

    @Override
    public void sendNotification(Long userId, String message, String type, Long relatedId, String link) {
        log.info("Sending notification to user {}: type={}, message={}", userId, type, message);

        // Validate inputs
        if (userId == null || userId <= 0) {
            throw new ValidationException("User ID must be a positive number");
        }
        if (!StringUtils.hasText(message)) {
            throw new ValidationException("Message cannot be empty");
        }
        if (!StringUtils.hasText(type)) {
            throw new ValidationException("Notification type cannot be empty");
        }

        try {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setMessage(message.trim());
            notification.setType(type.trim());
            notification.setRelatedId(relatedId);
            notification.setLink(link != null ? link.trim() : null);
            notification.setRead(false);

            Notification savedNotification = notificationRepository.save(notification);
            log.info("Notification sent successfully with ID: {}", savedNotification.getId());
        } catch (Exception e) {
            log.error("Failed to send notification to user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to send notification", e);
        }
    }

    @Override
    public List<NotificationResponse> getNotifications(Long userId) {
        log.debug("Fetching notifications for user: {}", userId);

        if (userId == null || userId <= 0) {
            throw new ValidationException("User ID must be a positive number");
        }

        try {
            List<NotificationResponse> notifications = notificationRepository
                    .findByUserIdOrderByCreatedAtDesc(userId)
                    .stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());

            log.debug("Found {} notifications for user {}", notifications.size(), userId);
            return notifications;
        } catch (Exception e) {
            log.error("Failed to fetch notifications for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch notifications", e);
        }
    }

    @Override
    public void markAsRead(Long notificationId) {
        log.debug("Marking notification {} as read", notificationId);

        if (notificationId == null || notificationId <= 0) {
            throw new ValidationException("Notification ID must be a positive number");
        }

        try {
            Notification notification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new NotificationNotFoundException(
                            "Notification not found with ID: " + notificationId));

            if (!notification.isRead()) {
                notification.setRead(true);
                notificationRepository.save(notification);
                log.info("Notification {} marked as read", notificationId);
            } else {
                log.debug("Notification {} was already read", notificationId);
            }
        } catch (NotificationNotFoundException e) {
            log.warn("Attempted to mark non-existent notification as read: {}", notificationId);
            throw e;
        } catch (Exception e) {
            log.error("Failed to mark notification {} as read: {}", notificationId, e.getMessage(), e);
            throw new RuntimeException("Failed to mark notification as read", e);
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        if (notification == null) {
            return null;
        }

        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setMessage(notification.getMessage());
        response.setRead(notification.isRead());
        response.setCreatedAt(notification.getCreatedAt());
        response.setType(notification.getType());
        response.setRelatedId(notification.getRelatedId());
        response.setLink(notification.getLink());
        return response;
    }
}