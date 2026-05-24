package com.example.exception.general;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.HASH_GENERATION_ERROR;

public class GenerateHashException extends BusinessException {
    public GenerateHashException() {
        super(HASH_GENERATION_ERROR);
    }
    public GenerateHashException(String message) {
        super(HASH_GENERATION_ERROR, message);
    }
}
