package com.example.exception.payment;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.CURRENCY_MISMATCHED;

public class MismatchedCurrencyException extends BusinessException {
    public MismatchedCurrencyException() {
        super(CURRENCY_MISMATCHED);
    }
    public MismatchedCurrencyException(String message) {
        super(CURRENCY_MISMATCHED, message);
    }
}
