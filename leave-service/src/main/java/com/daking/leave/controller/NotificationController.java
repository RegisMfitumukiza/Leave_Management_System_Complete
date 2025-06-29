package com.daking.leave.controller;

import com.daking.leave.dto.response.NotificationResponse;
import com.daking.leave.service.interfaces.InAppNotificationService;
// import com.daking.leave.client.UserInfoClient;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final InAppNotificationService notificationService;
    // private final UserInfoClient userInfoClient;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal String userIdStr) {
        Long userId = getUserIdFromPrincipal(userIdStr);
        return ResponseEntity.ok(notificationService.getNotifications(userId));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Integer> getUnreadCount(@AuthenticationPrincipal String userIdStr) {
        Long userId = getUserIdFromPrincipal(userIdStr);
        int count = notificationService.getNotifications(userId).stream().filter(n -> !n.isRead()).toArray().length;
        return ResponseEntity.ok(count);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<NotificationResponse>> getRecentNotifications(
            @AuthenticationPrincipal String userIdStr) {
        Long userId = getUserIdFromPrincipal(userIdStr);
        List<NotificationResponse> all = notificationService.getNotifications(userId);
        return ResponseEntity.ok(all.stream().limit(10).toList());
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal String userIdStr) {
        Long userId = getUserIdFromPrincipal(userIdStr);
        notificationService.getNotifications(userId).forEach(n -> notificationService.markAsRead(n.getId()));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/mark-read/{id}")
    public ResponseEntity<Void> markAsReadPut(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }

    private Long getUserIdFromPrincipal(String userIdStr) {
        if (userIdStr == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        try {
            return Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid user ID format");
        }
    }
}