package com.example.exception.payment;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.PAYMENT_PROCESSING_ERROR;

public class PaymentProcessingException extends BusinessException {
    public PaymentProcessingException() {
        super(PAYMENT_PROCESSING_ERROR);
    }
    public PaymentProcessingException(String message) {
        super(PAYMENT_PROCESSING_ERROR, message);
    }
}
