package com.np3.ledgerai.web.error;

import com.np3.ledgerai.domain.exception.AccountNotFoundException;
import com.np3.ledgerai.domain.exception.AiAdvisorException;
import com.np3.ledgerai.domain.exception.InvalidStateTransitionException;
import com.np3.ledgerai.domain.exception.JournalEntryNotFoundException;
import com.np3.ledgerai.domain.exception.NoActivityToAnalyzeException;
import com.np3.ledgerai.domain.exception.UnbalancedEntryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

@RestControllerAdvice
public class DomainExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(DomainExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationApiError> handleValidation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ValidationApiError.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        var body = new ValidationApiError(HttpStatus.BAD_REQUEST.value(), "Validation failed", Instant.now(), errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(UnbalancedEntryException.class)
    public ResponseEntity<ApiError> handleUnbalancedEntry(UnbalancedEntryException ex) {
        return build(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ApiError> handleInvalidStateTransition(InvalidStateTransitionException ex) {
        return build(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiError> handleAccountNotFound(AccountNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(JournalEntryNotFoundException.class)
    public ResponseEntity<ApiError> handleJournalEntryNotFound(JournalEntryNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(NoActivityToAnalyzeException.class)
    public ResponseEntity<ApiError> handleNoActivityToAnalyze(NoActivityToAnalyzeException ex) {
        return build(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(AiAdvisorException.class)
    public ResponseEntity<ApiError> handleAiAdvisor(AiAdvisorException ex) {
        // The provider failed, not the caller: 502, with the cause kept in the logs only.
        log.warn("AI advisor call failed", ex);
        return build(HttpStatus.BAD_GATEWAY, ex);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return buildMessage(HttpStatus.BAD_REQUEST, "Malformed request body");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return buildMessage(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + ex.getName() + "'");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, ex);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, Exception ex) {
        return buildMessage(status, ex.getMessage());
    }

    private ResponseEntity<ApiError> buildMessage(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiError(status.value(), message, Instant.now()));
    }
}