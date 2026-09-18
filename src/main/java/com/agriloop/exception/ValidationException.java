package com.agriloop.exception;

/**
 * Thrown when input data or business constraint validation fails.
 */
public class ValidationException extends AgriLoopException {
    public ValidationException(String message) {
        super(message);
    }
}
