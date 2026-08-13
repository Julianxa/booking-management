package com.example.exception;

import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Object> handleBusinessException(BusinessException ex, WebRequest request) {

        Map<String, Object> body = new HashMap<>();
        body.put("code", ex.getCode());
        body.put("message", ex.getMessage());
        body.put("timestamp", ZonedDateTime.now());

        // Return correct HTTP status + error body
        return new ResponseEntity<>(body, ex.getHttpStatus());
    }

    @ExceptionHandler(StripeException.class)
    public ResponseEntity<Object> handleStripeException(StripeException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", ex.getCode());
        body.put("message", ex.getMessage());
        body.put("timestamp", ZonedDateTime.now());
        return new ResponseEntity<>(body, ex.getHttpStatus());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex) {
        return toErrorResponse(ErrorCode.INVALID_ARGUMENT, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return toErrorResponse(ErrorCode.MISSING_REQUIRED_FIELD, message);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {
        String requestPath = request != null ? request.getRequestURI() : null;
        List<String> currentRoles = SecurityContextHolder.getContext().getAuthentication() == null
                ? List.of()
                : SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
                .toList();
        List<String> requiredRoles = resolveRequiredRoles(requestPath);

        String message = String.format(
                "Access denied. current_roles=%s, required_roles=%s",
                currentRoles,
                requiredRoles);
        return toErrorResponse(ErrorCode.ACCESS_DENIED, message);
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<Object> handleAuthenticationCredentialsNotFound(
            AuthenticationCredentialsNotFoundException ex) {
        return toErrorResponse(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getDefaultMessage());
    }

    // Fallback for any other unexpected exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralException(Exception ex) {
        log.error("Unhandled exception", ex);
        return toErrorResponse(ErrorCode.UNHANDLED_ERROR, ErrorCode.UNHANDLED_ERROR.getDefaultMessage());
    }

    private static ResponseEntity<Object> toErrorResponse(ErrorDefinition error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", error.getCode());
        body.put("message", message != null && !message.isBlank() ? message : error.getDefaultMessage());
        body.put("timestamp", ZonedDateTime.now());
        return new ResponseEntity<>(body, error.getStatus());
    }

    private List<String> resolveRequiredRoles(String requestPath) {
        if (requestPath == null || requestPath.isBlank()) {
            return List.of();
        }
        if (requestPath.endsWith("/events/admin-confirm")) {
            return Arrays.asList("EMPLOYEE", "ADMIN");
        }
        return List.of();
    }
}
