package com.example.exception.user;

import com.example.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static com.example.exception.ErrorCode.INVALID_ACCCESS_TOKEN;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidAccessTokenException extends BusinessException {

    public InvalidAccessTokenException() {
        super(INVALID_ACCCESS_TOKEN);
    }
    public InvalidAccessTokenException(String message) {
        super(INVALID_ACCCESS_TOKEN, message);
    }
}