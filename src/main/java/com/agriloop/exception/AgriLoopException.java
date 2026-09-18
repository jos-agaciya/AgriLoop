package com.agriloop.exception;

/**
 * Base root exception for all AgriLoop domain and runtime errors.
 */
public class AgriLoopException extends RuntimeException {
    public AgriLoopException(String message) {
        super(message);
    }

    public AgriLoopException(String message, Throwable cause) {
        super(message, cause);
    }
}
