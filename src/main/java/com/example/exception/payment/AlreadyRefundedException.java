package com.example.exception.payment;

import com.example.exception.BusinessException;

public class AlreadyRefundedException extends BusinessException {
    public AlreadyRefundedException(String message) {
        super("BT503", message);
    }

}
