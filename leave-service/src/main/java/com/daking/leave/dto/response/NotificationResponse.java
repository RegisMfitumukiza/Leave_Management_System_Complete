package com.daking.leave.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationResponse {
    private Long id;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;
    private String type;
    private Long relatedId;
    private String link;
}