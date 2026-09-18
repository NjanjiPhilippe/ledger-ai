package com.np3.ledgerai.web.error;

import com.np3.ledgerai.domain.exception.AccountNotFoundException;
import com.np3.ledgerai.domain.exception.InvalidStateTransitionException;
import com.np3.ledgerai.domain.exception.JournalEntryNotFoundException;
import com.np3.ledgerai.domain.exception.UnbalancedEntryException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class DomainExceptionHandler {

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

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, ex);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, Exception ex) {
        return ResponseEntity.status(status).body(new ApiError(status.value(), ex.getMessage(), Instant.now()));
    }
}
