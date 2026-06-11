package com.example.exception.email;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.INTERVAL_NOT_FOUND;

public class MissingIntervalException extends BusinessException {
    public MissingIntervalException() {
        super(INTERVAL_NOT_FOUND);
    }
    public MissingIntervalException(String message) {
        super(INTERVAL_NOT_FOUND, message);
    }

}
