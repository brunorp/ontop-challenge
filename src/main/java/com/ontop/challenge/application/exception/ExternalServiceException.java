package com.ontop.challenge.application.exception;

/**
 * Exception thrown when external service calls fail
 * Application-level exception that can be thrown by any port implementation
 */
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

