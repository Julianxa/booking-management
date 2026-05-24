package com.example.exception.general;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.PARSE_TOKEN_ERROR;

public class ParseTokenException extends BusinessException {
    public ParseTokenException() {
        super(PARSE_TOKEN_ERROR);
    }
    public ParseTokenException(String message) {
        super(PARSE_TOKEN_ERROR, message);
    }
}
