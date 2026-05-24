package com.example.exception.giftCertificate;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.GC_NOT_FOUND;

public class GCNotFoundException extends BusinessException {
    public GCNotFoundException() {
        super(GC_NOT_FOUND);
    }
    public GCNotFoundException(String message) {
        super(GC_NOT_FOUND, message);
    }
}
