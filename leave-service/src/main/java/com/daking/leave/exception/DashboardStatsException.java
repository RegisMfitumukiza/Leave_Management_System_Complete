package com.daking.leave.exception;

public class DashboardStatsException extends RuntimeException {
    public DashboardStatsException(String message) {
        super(message);
    }

    public DashboardStatsException(String message, Throwable cause) {
        super(message, cause);
    }
}