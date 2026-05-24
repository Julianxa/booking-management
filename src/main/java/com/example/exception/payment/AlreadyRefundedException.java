package com.example.exception.payment;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.REFUNDED_ALREADY;

public class AlreadyRefundedException extends BusinessException {
    public AlreadyRefundedException() {
        super(REFUNDED_ALREADY);
    }
    public AlreadyRefundedException(String message) {
        super(REFUNDED_ALREADY, message);
    }
}
