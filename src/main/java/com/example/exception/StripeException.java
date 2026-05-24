package com.example.exception;


import org.springframework.http.HttpStatus;

public class StripeException extends RuntimeException {
    private final String code;
    private final HttpStatus httpStatus;

    public StripeException(ErrorDefinition error) {
        super(error.getDefaultMessage());
        this.code = error.getCode();
        this.httpStatus = error.getStatus();
    }

    public StripeException(ErrorDefinition error, String message) {
        super(message);
        this.code = error.getCode();
        this.httpStatus = error.getStatus();
    }

    public String getCode() { return code; }
    public HttpStatus getHttpStatus() { return httpStatus; }
}
