package com.example.exception.giftCertificate;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.INVALID_GC;

public class InvalidGCException extends BusinessException {
    public InvalidGCException() {
        super(INVALID_GC);
    }
    public InvalidGCException(String message) {
        super(INVALID_GC, message);
    }
}
