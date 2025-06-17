package com.daking.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateDepartmentException extends RuntimeException {
    public DuplicateDepartmentException(String message) {
        super(message);
    }

    public DuplicateDepartmentException(String message, Throwable cause) {
        super(message, cause);
    }
}