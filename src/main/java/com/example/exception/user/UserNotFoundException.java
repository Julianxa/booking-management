package com.example.exception.user;

import com.example.exception.BusinessException;

public class UserNotFoundException extends BusinessException {
    public UserNotFoundException(String message) {
        super("BT906", message);
    }

}
