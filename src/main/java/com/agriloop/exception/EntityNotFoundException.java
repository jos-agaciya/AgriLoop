package com.agriloop.exception;

/**
 * Thrown when an expected entity cannot be found in the database.
 */
public class EntityNotFoundException extends AgriLoopException {
    public EntityNotFoundException(String entityName, Object id) {
        super(String.format("Entity '%s' with identifier '%s' was not found.", entityName, id));
    }
}
