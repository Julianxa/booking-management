package com.example.exception.general;

import com.example.exception.BusinessException;

public class GenerateQRCodeException extends BusinessException {
    public GenerateQRCodeException(String message) {
        super("BT004", message);
    }

}
