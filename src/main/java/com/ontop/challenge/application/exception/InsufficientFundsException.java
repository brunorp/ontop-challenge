package com.ontop.challenge.application.exception;

/**
 * Exception thrown when user has insufficient funds for withdrawal
 */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message) {
        super(message);
    }

    public InsufficientFundsException(String message, Throwable cause) {
        super(message, cause);
    }
}

