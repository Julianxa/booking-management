package com.example.exception.general;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.RANDOM_REF_NO_ERROR;

public class RandomReferenceNoException extends BusinessException {
    public RandomReferenceNoException() {
        super(RANDOM_REF_NO_ERROR);
    }
    public RandomReferenceNoException(String message) {
        super(RANDOM_REF_NO_ERROR, message);
    }
}
