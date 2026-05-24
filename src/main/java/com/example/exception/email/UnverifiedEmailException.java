package com.example.exception.email;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.UNVERIFIED_EMAIL;

public class UnverifiedEmailException extends BusinessException {
    public UnverifiedEmailException() {
        super(UNVERIFIED_EMAIL);
    }
    public UnverifiedEmailException(String message) {
        super(UNVERIFIED_EMAIL, message);
    }

}