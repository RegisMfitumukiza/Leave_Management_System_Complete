package com.daking.leave.service.impl;

import com.daking.leave.service.interfaces.NotificationService;
import com.daking.leave.service.interfaces.InAppNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private InAppNotificationService inAppNotificationService;

    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            logger.info("[EMAIL SENT] To: {} | Subject: {}", to, subject);
        } catch (Exception e) {
            logger.error("[EMAIL ERROR] Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

    @Override
    public void sendInAppNotification(Long userId, String message) {
        inAppNotificationService.sendNotification(userId, message, "INFO", null, null);
        logger.info("[IN-APP] UserId: {} | Message: {} (persisted)", userId, message);
    }

    public void sendInAppNotification(Long userId, String message, String type, Long relatedId, String link) {
        inAppNotificationService.sendNotification(userId, message, type, relatedId, link);
        logger.info("[IN-APP] UserId: {} | Message: {} | Type: {} | RelatedId: {} | Link: {} (persisted)", userId,
                message, type, relatedId, link);
    }
}