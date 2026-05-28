package com.example.exception.booking;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.THRESHOLD_EXCEEDED;

public class ThresholdExceededException extends BusinessException {
    public ThresholdExceededException() {
        super(THRESHOLD_EXCEEDED);
    }
    public ThresholdExceededException(String message) {
        super(THRESHOLD_EXCEEDED, message);
    }
}
