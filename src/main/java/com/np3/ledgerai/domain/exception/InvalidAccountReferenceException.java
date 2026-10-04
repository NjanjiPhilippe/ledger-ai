package com.np3.ledgerai.domain.exception;

public class InvalidAccountReferenceException extends RuntimeException {
    public InvalidAccountReferenceException(String message) {
        super(message);
    }
}
