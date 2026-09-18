package com.agriloop.exception;

/**
 * Thrown when a database or JDBC operation fails.
 */
public class DatabaseException extends AgriLoopException {
    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
