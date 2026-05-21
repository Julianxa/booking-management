package com.example.exception.user;

import com.example.exception.BusinessException;

public class UnverifiedEmailException extends BusinessException {
    public UnverifiedEmailException(String message) {
        super("BT905", message);
    }

}
