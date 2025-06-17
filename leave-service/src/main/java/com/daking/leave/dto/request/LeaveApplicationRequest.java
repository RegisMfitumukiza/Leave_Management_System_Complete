package com.daking.leave.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@Data
public class LeaveApplicationRequest {
    @NotNull
    private Long leaveTypeId;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    private String reason;

    @Size(max = 5)
    private List<Long> documentIds;
}