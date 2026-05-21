package com.example.exception.email;

public class UnverifiedEmailException extends Exception {
    public UnverifiedEmailException(String message) {
        super(message);
    }

    public UnverifiedEmailException(String message, Throwable cause) {
        super(message, cause);
    }
}