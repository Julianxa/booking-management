package com.example.exception.general;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.MISSING_REQUIRED_FIELD;

public class MissingRequiredFieldException extends BusinessException {
    public MissingRequiredFieldException() {
        super(MISSING_REQUIRED_FIELD);
    }
    public MissingRequiredFieldException(String message) {
        super(MISSING_REQUIRED_FIELD, message);
    }
}