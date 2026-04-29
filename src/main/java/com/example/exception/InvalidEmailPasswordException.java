package com.example.exception;

public class InvalidEmailPasswordException extends Exception {
    public InvalidEmailPasswordException(String message) {
        super(message);
    }

    public InvalidEmailPasswordException(String message, Throwable cause) {
        super(message, cause);
    }
}
