package com.np3.ledgerai.domain.exception;

public class NoActivityToAnalyzeException extends RuntimeException {
    public NoActivityToAnalyzeException() {
        super("No posted activity to analyze yet");
    }
}
