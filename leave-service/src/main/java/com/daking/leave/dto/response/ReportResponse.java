package com.daking.leave.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReportResponse {
    private Long id;
    private String name;
    private String type;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String generatedBy;
    private LocalDateTime generatedAt;
    private String fileType;
    private String error;

    public ReportResponse() {
    }

    public ReportResponse(String error) {
        this.error = error;
    }
}