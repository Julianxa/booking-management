package com.example.exception.user;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.INVALID_ID_TOKEN;

public class InvalidIdTokenException extends BusinessException {
    public InvalidIdTokenException() {
        super(INVALID_ID_TOKEN);
    }
    public InvalidIdTokenException(String message) {
        super(INVALID_ID_TOKEN, message);
    }
}

