package com.example.exception.general;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.INVALID_JSON_FORMAT;

public class InvalidJsonFormatException extends BusinessException {
    public InvalidJsonFormatException() {
        super(INVALID_JSON_FORMAT);
    }

    public InvalidJsonFormatException(String message) {
        super(INVALID_JSON_FORMAT, message);
    }
}
