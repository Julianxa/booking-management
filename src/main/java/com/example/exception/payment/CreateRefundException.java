package com.example.exception.payment;

import com.example.exception.BusinessException;

public class CreateRefundException extends BusinessException {
    public CreateRefundException(String message) {
        super("BT505", message);
    }

}
