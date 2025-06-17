package com.daking.leave.service.impl;

import com.daking.leave.dto.response.NotificationResponse;
import com.daking.leave.model.Notification;
import com.daking.leave.repository.NotificationRepository;
import com.daking.leave.service.interfaces.InAppNotificationService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InAppNotificationServiceImpl implements InAppNotificationService {
    private final NotificationRepository notificationRepository;

    public InAppNotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void sendNotification(Long userId, String message, String type, Long relatedId, String link) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRelatedId(relatedId);
        notification.setLink(link);
        notification.setRead(false);
        notificationRepository.save(notification);
    }

    @Override
    public List<NotificationResponse> getNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    private NotificationResponse toResponse(Notification n) {
        NotificationResponse resp = new NotificationResponse();
        resp.setId(n.getId());
        resp.setMessage(n.getMessage());
        resp.setRead(n.isRead());
        resp.setCreatedAt(n.getCreatedAt());
        resp.setType(n.getType());
        resp.setRelatedId(n.getRelatedId());
        resp.setLink(n.getLink());
        return resp;
    }
}