package com.example.exception;


public class StripeException extends RuntimeException {
    private final String errorCode;

    public StripeException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
