package com.example.exception.payment;

import com.example.exception.BusinessException;

public class PaymentNotFoundException extends BusinessException {
    public PaymentNotFoundException(String message) {
        super("BT805", message);
    }

}
