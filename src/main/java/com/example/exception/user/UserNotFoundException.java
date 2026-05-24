package com.example.exception.user;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.USER_NOT_FOUND;

public class UserNotFoundException extends BusinessException {
    public UserNotFoundException() {
        super(USER_NOT_FOUND);
    }

    public UserNotFoundException(String message) {
        super(USER_NOT_FOUND, message);
    }

}
