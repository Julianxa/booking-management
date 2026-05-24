package com.example.exception.general;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.INTERNAL_SERVER_ERROR;

public class InternalServerException extends BusinessException {
    public InternalServerException() {
        super(INTERNAL_SERVER_ERROR);
    }
    public InternalServerException(String message) {
        super(INTERNAL_SERVER_ERROR, message);
    }

}
