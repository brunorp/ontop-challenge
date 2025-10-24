package com.ontop.challenge.application.exception;

/**
 * Exception thrown when there's a conflict with idempotency key
 */
public class IdempotentConflictException extends RuntimeException {

    public IdempotentConflictException(String message) {
        super(message);
    }

    public IdempotentConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}

