package com.example.exception;

public class InvalidIdTokenException extends Exception {
    public InvalidIdTokenException(String message) {
        super(message);
    }

    public InvalidIdTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}

