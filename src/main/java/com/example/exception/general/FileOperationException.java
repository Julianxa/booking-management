package com.example.exception.general;

import com.example.exception.BusinessException;

public class FileOperationException extends BusinessException {
    public FileOperationException(String message) {
        super("BT003", message);
    }

}
