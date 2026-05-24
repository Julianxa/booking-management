package com.example.exception.user;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.INVALID_EMAIL_PASSWORD;

public class InvalidEmailPasswordException extends BusinessException {
    public InvalidEmailPasswordException() {
        super(INVALID_EMAIL_PASSWORD);
    }
    public InvalidEmailPasswordException(String message) {
        super(INVALID_EMAIL_PASSWORD, message);
    }
}
