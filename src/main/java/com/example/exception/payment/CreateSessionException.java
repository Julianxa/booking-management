package com.example.exception.payment;

import com.example.exception.BusinessException;
import com.example.exception.StripeException;

public class CreateSessionException extends StripeException {
    public CreateSessionException(String message) {
        super("BT803", message);
    }

}
