package com.example.exception.email;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.EMAIL_PROCESSING_ERROR;

public class EmailProcessException extends BusinessException {
    public EmailProcessException() {
        super(EMAIL_PROCESSING_ERROR);
    }
    public EmailProcessException(String message) {
        super(EMAIL_PROCESSING_ERROR, message);
    }
}
