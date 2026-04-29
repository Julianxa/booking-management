package com.example.exception;

public class InvalidVerificationTokenException extends Exception {
    public InvalidVerificationTokenException(String message) {
        super(message);
    }

    public InvalidVerificationTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}

