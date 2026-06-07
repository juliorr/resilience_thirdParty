package com.incode.verification.api;

import com.incode.verification.persistence.DuplicateVerificationException;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParameter(
            MissingServletRequestParameterException exception) {
        log.debug("Bad request: {}", exception.getMessage());
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String expected = exception.getRequiredType() == UUID.class
                ? "GUID"
                : (exception.getRequiredType() == null
                        ? "value"
                        : exception.getRequiredType().getSimpleName());
        String message = exception.getName() + " must be a valid " + expected;
        log.debug("Bad request: {}", message);
        return problem(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(HandlerMethodValidationException exception) {
        String message = exception.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Invalid request");
        log.debug("Bad request: {}", message);
        return problem(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(VerificationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(VerificationNotFoundException exception) {
        log.debug("Verification not found: {}", exception.getMessage());
        return problem(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(DuplicateVerificationException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(DuplicateVerificationException exception) {
        log.debug("Duplicate verification: {}", exception.getMessage());
        return problem(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException exception) {
        log.debug("Access denied: {}", exception.getMessage());
        return problem(HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN.getReasonPhrase());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException exception) {
        log.debug("Authentication failed: {}", exception.getMessage());
        return problem(HttpStatus.UNAUTHORIZED, HttpStatus.UNAUTHORIZED.getReasonPhrase());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception exception) {
        log.error("Unhandled exception processing request", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    private ResponseEntity<Map<String, Object>> problem(HttpStatus status, String message) {
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message == null ? status.getReasonPhrase() : message);
        return ResponseEntity.status(status).body(body);
    }
}
