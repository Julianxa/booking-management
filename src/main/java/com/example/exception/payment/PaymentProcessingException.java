package com.example.exception.payment;

import com.example.exception.BusinessException;

public class PaymentProcessingException extends BusinessException {
    public PaymentProcessingException(String message) {
        super("BT806", message);
    }
}
