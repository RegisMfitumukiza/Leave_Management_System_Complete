package com.daking.leave.dto.response;

import lombok.Data;
import java.time.LocalDate;

@Data
public class HolidayResponse {
    private Long id;
    private String name;
    private LocalDate date;
    private String description;
    private boolean isPublic;
}