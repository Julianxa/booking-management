package com.example.exception.ticket;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.INVALID_VERIFICATION_TOKEN;

public class InvalidVerificationTokenException extends BusinessException {
    public InvalidVerificationTokenException() {
        super(INVALID_VERIFICATION_TOKEN);
    }
    public InvalidVerificationTokenException(String message) {
        super(INVALID_VERIFICATION_TOKEN, message);
    }
}
