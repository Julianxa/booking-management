package com.example.exception;

import com.example.model.dto.ErrorResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDTO> handleBusinessException(BusinessException e) {
        HttpStatus status = determineHttpStatus(e.getErrorCode());

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now().toString())
                .errorCode(e.getErrorCode())
                .message(e.getMessage())
                .traceId(UUID.randomUUID().toString())
                .build();

        log.warn("Business Exception [{}] - {}", e.getErrorCode(), e.getMessage());
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse("Validation failed");

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now().toString())
                .errorCode("EV-9998")
                .message("Validation failed: " + message)
                .traceId(UUID.randomUUID().toString())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unexpected exception occurred: {}", ex.getMessage(), ex);

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now().toString())
                .errorCode("EV-9999")
                .message("An unexpected error occurred. Please contact support.")
                .traceId(UUID.randomUUID().toString())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private HttpStatus determineHttpStatus(String errorCode) {
        if (errorCode == null || errorCode.isEmpty()) {
            return HttpStatus.BAD_REQUEST;
        }

        String prefix = errorCode.substring(0, Math.min(5, errorCode.length()));

        return switch (prefix) {
            case "EV-01" -> HttpStatus.NOT_FOUND;           // Resource not found
            case "EV-02" -> HttpStatus.BAD_REQUEST;         // Validation / JSON issues
            case "EV-03" -> HttpStatus.BAD_REQUEST;         // Booking related
            case "EV-04" -> HttpStatus.CONFLICT;            // Payment / Already exists / Refund issues
            case "EV-05" -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    /**
     * Optional: Build error helper (can be used by other handlers if needed)
     */
    private ResponseEntity<ErrorResponseDTO> buildError(HttpStatus status, String errorCode, String message) {
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now().toString())
                .errorCode(errorCode)
                .message(message)
                .traceId(UUID.randomUUID().toString())
                .build();

        return ResponseEntity.status(status).body(error);
    }
}
