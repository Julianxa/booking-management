package com.example.exception.email;

import com.example.exception.BusinessException;

public class UnverifiedEmailException extends BusinessException {
    public UnverifiedEmailException(String message) {
        super("BT503", message);
    }
}