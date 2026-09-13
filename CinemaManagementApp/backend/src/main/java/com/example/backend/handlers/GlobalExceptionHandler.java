package com.example.backend.handlers;

import com.example.backend.dto.ApiErrorResponse;
import com.example.backend.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    //HELPER
    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message, HttpServletRequest req, Map<String, Object> details) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                req.getRequestURI(),
                details
        );
        return ResponseEntity.status(status).body(body);
    }

    //VALIDATION (@VALID)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, Object> details = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();

        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }

        details.put("fieldErrors", fieldErrors);
        return build(HttpStatus.BAD_REQUEST, "Validation failed", req, details);
    }

    //VALIDATION (QUERY PARAMS / PATH PARAMS CONSTRAINTS)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        Map<String, Object> details = new HashMap<>();
        details.put("violations", ex.getConstraintViolations().stream().map(v -> Map.of(
                "property", v.getPropertyPath().toString(),
                "message", v.getMessage()
        )).toList());

        return build(HttpStatus.BAD_REQUEST, "Validation failed", req, details);
    }

    //MALFORMED JSON
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleBadJson(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Malformed JSON request body", req, null);
    }

    //OUR EXPLICIT EXCEPTIONS
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(UnauthorizedException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req, null);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiErrorResponse> handleForbidden(ForbiddenException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), req, null);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    //FALLBACK: MAP YOUR EXISTING RUNTIMEEXCEPTION MESSAGES
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntime(RuntimeException ex, HttpServletRequest req) {
        String msg = (ex.getMessage() == null) ? "Unexpected error" : ex.getMessage();

        //VERY COMMON PATTERNS IN YOUR CODEBASE:
        String m = msg.toLowerCase();

        if (m.contains("forbidden")) {
            return build(HttpStatus.FORBIDDEN, msg, req, null);
        }
        if (m.contains("not found")) {
            return build(HttpStatus.NOT_FOUND, msg, req, null);
        }
        if (m.contains("already exists") || m.contains("conflict") || m.contains("overlap")) {
            return build(HttpStatus.CONFLICT, msg, req, null);
        }
        if (m.contains("no authenticated user") || m.contains("unauthorized") || m.contains("invalid token") || m.contains("token")) {
            return build(HttpStatus.UNAUTHORIZED, msg, req, null);
        }

        //DEFAULT: 400 (DOMAIN RULE VIOLATION / BAD REQUEST)
        return build(HttpStatus.BAD_REQUEST, msg, req, null);
    }

    //LAST RESORT
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleAny(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", req, null);
    }
}

