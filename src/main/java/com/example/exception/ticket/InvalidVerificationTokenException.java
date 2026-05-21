package com.example.exception.ticket;

import com.example.exception.BusinessException;

public class InvalidVerificationTokenException extends BusinessException {
    public InvalidVerificationTokenException(String message) {
        super("BT901", message);
    }

}
