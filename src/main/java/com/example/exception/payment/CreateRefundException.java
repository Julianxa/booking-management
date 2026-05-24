package com.example.exception.payment;

import com.example.exception.StripeException;

import static com.example.exception.ErrorCode.CREATE_REFUND_ERROR;

public class CreateRefundException extends StripeException {
    public CreateRefundException() {
        super(CREATE_REFUND_ERROR);
    }
    public CreateRefundException(String message) {
        super(CREATE_REFUND_ERROR, message);
    }
}
