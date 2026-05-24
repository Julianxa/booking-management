package com.example.exception.general;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.FILE_OP_ERROR;

public class FileOperationException extends BusinessException {
    public FileOperationException() {
        super(FILE_OP_ERROR);
    }
    public FileOperationException(String message) {
        super(FILE_OP_ERROR, message);
    }
}
