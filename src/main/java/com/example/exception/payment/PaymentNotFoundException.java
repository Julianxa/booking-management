package com.example.exception.payment;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.PAYMENT_NOT_FOUND;

public class PaymentNotFoundException extends BusinessException {
    public PaymentNotFoundException() {
        super(PAYMENT_NOT_FOUND);
    }
    public PaymentNotFoundException(String message) {
        super(PAYMENT_NOT_FOUND, message);
    }
}
