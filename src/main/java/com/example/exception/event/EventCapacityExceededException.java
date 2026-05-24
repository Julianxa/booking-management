package com.example.exception.event;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.CAPACITY_EXCEEDED;

public class EventCapacityExceededException extends BusinessException {

    public EventCapacityExceededException() {
        super(CAPACITY_EXCEEDED);
    }
    public EventCapacityExceededException(String message) {
        super(CAPACITY_EXCEEDED, message);
    }

}