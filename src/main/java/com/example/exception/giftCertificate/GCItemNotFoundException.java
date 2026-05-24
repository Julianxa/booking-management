package com.example.exception.giftCertificate;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.GC_ITEM_NOT_FOUND;

public class GCItemNotFoundException extends BusinessException {
    public GCItemNotFoundException() {
        super(GC_ITEM_NOT_FOUND);
    }
    public GCItemNotFoundException(String message) {
        super(GC_ITEM_NOT_FOUND, message);
    }
}
