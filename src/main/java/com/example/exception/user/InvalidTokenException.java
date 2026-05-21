package com.example.exception.user;

import com.example.exception.BusinessException;

public class InvalidTokenException extends BusinessException {
    public InvalidTokenException(String message) {
        super("BT904", message);
    }

}
