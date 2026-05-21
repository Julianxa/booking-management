package com.example.exception.payment;

import com.example.exception.BusinessException;

public class MismatchedCurrencyException extends BusinessException {
    public MismatchedCurrencyException(String message) {
        super("BT504", message);
    }

}
