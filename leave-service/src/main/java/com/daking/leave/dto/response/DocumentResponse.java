package com.daking.leave.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DocumentResponse {
    private Long id;
    private Long userId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String url;
    private LocalDateTime createdAt;
    private String status;
    private String employeeName;
    private String leaveTypeName;
}