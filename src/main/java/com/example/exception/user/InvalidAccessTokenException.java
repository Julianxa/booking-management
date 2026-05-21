package com.example.exception.user;

import com.example.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidAccessTokenException extends BusinessException {

    public InvalidAccessTokenException(String message) {
        super("BT901", message);
    }
}