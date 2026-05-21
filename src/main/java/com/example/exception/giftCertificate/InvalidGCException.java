package com.example.exception.giftCertificate;

import com.example.exception.BusinessException;

public class InvalidGCException extends BusinessException {
    public InvalidGCException(String message) {
        super("BT705", message);
    }

}
