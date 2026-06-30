package com.example.exception.report;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.REPORT_NOT_FOUND;

public class ReportNotFoundException extends BusinessException {
    public ReportNotFoundException() {
        super(REPORT_NOT_FOUND);
    }

    public ReportNotFoundException(String message) {
        super(REPORT_NOT_FOUND, message);
    }
}
