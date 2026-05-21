package com.example.exception.user;

import com.example.exception.BusinessException;

public class InvalidEmailPasswordException extends BusinessException {
    public InvalidEmailPasswordException(String message) {
        super("BT602", message);
    }
}
