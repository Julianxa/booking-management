package com.example.exception.payment;

import com.example.exception.StripeException;

import static com.example.exception.ErrorCode.CREATE_SESSION_ERROR;

public class CreateSessionException extends StripeException {
    public CreateSessionException() {
        super(CREATE_SESSION_ERROR);
    }
    public CreateSessionException(String message) {
        super(CREATE_SESSION_ERROR, message);
    }
}
