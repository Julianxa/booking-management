package com.example.exception.payment;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.REFUND_NOT_FOUND;

public class RefundNotFoundException extends BusinessException {
    public RefundNotFoundException() {
        super(REFUND_NOT_FOUND);
    }
    public RefundNotFoundException(String message) {
        super(REFUND_NOT_FOUND, message);
    }
}