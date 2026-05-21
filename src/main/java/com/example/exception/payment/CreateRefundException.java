package com.example.exception.payment;

import com.example.exception.StripeException;

public class CreateRefundException extends StripeException {
    public CreateRefundException(String message) {
        super("BT802", message);
    }

}
