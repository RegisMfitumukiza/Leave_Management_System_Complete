package com.daking.leave.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class LeaveResponse {
    private Long id;
    private Long userId;
    private Long leaveTypeId;
    private String leaveTypeName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double totalDays;
    private String status;
    private String reason;
    private String comments;
    private Long approverId;
    private List<Long> documentIds;
    private List<DocumentResponse> documents;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String employeeName;
    private String leaveType;
}