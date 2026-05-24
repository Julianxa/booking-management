package com.example.exception;

import org.springframework.http.HttpStatus;

public class ErrorDefinition {
    private final String code;
    private final HttpStatus status;
    private final String defaultMessage;

    public ErrorDefinition(String code, HttpStatus status, String defaultMessage) {
        this.code = code;
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() { return code; }
    public HttpStatus getStatus() { return status; }
    public String getDefaultMessage() { return defaultMessage; }
}
