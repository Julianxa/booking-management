package com.example.exception.user;

import com.example.exception.BusinessException;

public class InvalidIdTokenException extends BusinessException {
    public InvalidIdTokenException(String message) {
        super("BT903", message);
    }

}

